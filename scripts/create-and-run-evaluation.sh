#!/bin/bash
set -e # exit if a command fails

declare -a btcAddresses=("bcrt1qejv7wf8ax4lkag9w3hclyzu47zgj7q6eqv6s60" "bcrt1q30t4dep9laf3k8ys84xrx752awwuzt97gsvc3d" "bcrt1qzuey56rpj0eef2pjc4uz2hhwf30vkk83v2z433" "bcrt1q0gml6gq8q9ev80pqutqst25szpzgw7gsp72eke" "bcrt1qhwu5jng39tfehrxtjuhwc5wnv5n9sxrn0rc095" "bcrt1qjftzzcn2avx2t7lypjf5dpheu0pw08vse3vmlk" "bcrt1qvqcn4676wplcz8d73t8dgvxy8v269h4ra467lj" "bcrt1qt9w03gaxxcw9pnan4wx4enppxxlznvmuaranxx" "bcrt1qpa2vwlw60xp07zvs89hs3xeff6xshv6sp00f6s" "bcrt1qu3sdjufevlnm7wuld5a7qague5mcwhkdz3swfr")
GENADDR="bcrt1qyy44rea9a79y8y0fhhnttvype6wcz40gpqgg0h"

function buildDirectoryName {
    confStr="confirmed"
    waitStr="waitblocks"
    if [ $unconfirmed == 1 ]; then
        confStr="unconfirmed"
        waitStr="waitseconds"
    fi
    vetoStr="novetos"
    if [ $veto \> 0 ]; then
        vetoStr="vetorun"$veto
    fi

    directoryName="evaldata/"$confStr"/chainoffset"$chainoffset"/"$waitStr$wait"/txtime"$txtime$vetoStr
}


echo "Starting..."
startprograms=0
originalPath=$(pwd)
runtime=83077
runtimeGenerate=85177 # 35 minutes longer... (3 blocks + additional time until everything gets started)

failedConfFile="failedconfigurations.txt"
if [ -f $failedConfFile ]; then
    readarray -t pathsToCreate < $failedConfFile
fi


configs=0
nextPort=18443
for unconfirmed in {0..1}; do
    for chainoffset in 0 200 300; do #in seconds. => 0m, 3.333m, 5m
        for txtime in {231..3234..231}; do
        
            if [ $txtime == 3234 ]; then
                # range={0..5} #blocks
                startRange=0
                endRange=5
                inc=1
                if [ $unconfirmed == 1 ]; then
                    # range={0..30000..10000} #milliseconds
                    startRange=0
                    endRange=30000
                    inc=10000
                fi
            else
                # range={1} #blocks
                startRange=1
                endRange=1
                inc=1
                if [ $unconfirmed == 1 ]; then
                    #range={10000} #milliseconds
                    startRange=10000
                    endRange=10000
                    inc=10000
                fi
            fi

            for (( wait=$startRange; wait<=$endRange; wait+=$inc)); do

                vetoEndRange=0
                if [[ $unconfirmed == 0 && $chainoffset == 0 && $wait == 1 && $txtime == 3234 ]]; then
                    # start veto cost eval for this configuration
                    vetoEndRange=1
                fi

                for (( veto=0; veto<=$vetoEndRange; veto++ )); do
                    # do stuff here
                    cd "$originalPath"
                    buildDirectoryName

                    if [ ! -z "$pathsToCreate" ]; then
                        if [[ ! " ${pathsToCreate[@]} " =~ " ${directoryName} " ]]; then
                            echo "Configuration not in ""$failedConfFile"": ""$directoryName"
                            continue
                        fi
                    fi

                    if [ ! -d "$directoryName" ]; then
                        mkdir -p "$directoryName"
                        cd "$directoryName"

                        dexxtconfig="configuration.csv"
                        touch $dexxtconfig
                        echo "unconfirmed,chainoffset,wait,txtime,veto" >> $dexxtconfig
                        echo $unconfirmed","$chainoffset","$wait","$txtime","$veto >> $dexxtconfig

                        # setup bitcoin core instances
                        declare -a rpcports=()
                        for i in {1..3}; do
                            rpcport=$nextPort
                            ((nextPort+=1))
                            port=$nextPort
                            ((nextPort+=1))

                            rpcports[$i]=$rpcport

                            btcDir=.bitcoin"$i"
                            mkdir $btcDir
                            configFile=$btcDir/bitcoin.conf
                            touch $configFile
                            echo "regtest=1" >> $configFile
                            echo "server=1" >> $configFile
                            echo "rpcauth=matthias:1f741caad2e49f6cd1030a4dffc331cd\$2d65b06f1b573c899edec705447e9c066dc2c33fe7bb5369fa39b56d617126ad" >> $configFile # matthias:password
                            echo "rpcworkqueue=256" >> $configFile # default of 16 got exceeded regularly -> many requests on new block in parallel
                            echo "txindex=1" >> $configFile
                            echo "[regtest]" >> $configFile
                            echo "rpcconnect=localhost" >> $configFile
                            echo "rpcport=""$rpcport" >> $configFile
                            echo "port=""$port" >> $configFile

                            if [ $startprograms == 1 ]; then
                                datadir="$originalPath""/""$directoryName""/""$btcDir"
                                bitcoind -datadir="$datadir" -daemon
                                echo "Started bitcoind with: ""$datadir"
                            fi
                        done

                        if [ $startprograms == 1 ]; then
                            for i in {1..3}; do
                                btcDir=.bitcoin"$i"
                                datadir="$originalPath""/""$directoryName""/""$btcDir"
                                until bitcoin-cli -datadir="$datadir" getblockcount &>/dev/null; do
                                    echo "bitcoind not running yet..."
                                    sleep 1
                                done
                                echo "bitcoind running, starting setup..."
                                "$originalPath"/bitcoin-setup.sh "$datadir" &>/dev/null
                            done
                        fi

                        urls="--urlrpc http://matthias:password@127.0.0.1:"${rpcports[1]}"/,http://matthias:password@127.0.0.1:"${rpcports[2]}"/,http://matthias:password@127.0.0.1:"${rpcports[3]}"/"
                        # create argumentfiles for DeXTT-BTC
                        touch "mint.txt"
                        echo $urls >> "mint.txt"
                        echo "--address bcrt1qejv7wf8ax4lkag9w3hclyzu47zgj7q6eqv6s60 --chain REGTEST mint" >> "mint.txt"
                        echo "-m caae72556fc36d3a8c859029047119b712717419,ca43f8812da1a4219d91e83a6af49d7dfd7c9a67,3d716c6f95050fc1aacac4ae6b1da4b71bdbfdcc,7bafc011aa9a94f5a2106a8d55d0196796fe5e84,1e915de3c3f543b33d1e290ecf797ca0299eae1e,4ede41dd3b87029025249a27fc04c9f971741c69,f177543aa32d9f579fd21ef72e56004a51565ae1,c708ab808319d9169f3cfbeccaf375e18a6e7794,b61c76a540fa2ad239dbcd4215e43f6aafe6d3bb,6c5cee6ebe9ef4868207ca51b3cfd6b4473f416d" >> "mint.txt"
                        echo "--amount 5000" >> "mint.txt"

                        touch "generate.txt"
                        echo $urls >> "generate.txt"
                        echo "--address bcrt1qyy44rea9a79y8y0fhhnttvype6wcz40gpqgg0h --chain REGTEST generateblocks" >> "generate.txt"
                        echo "--runtime "$runtimeGenerate" --blockInterval 600 --chainOffset "$chainoffset >> "generate.txt"
                        #echo "--runtime "$runtimeGenerate" --blockInterval 30 --chainOffset "$chainoffset >> "generate.txt"

                        if [ $startprograms == 1 ]; then
                            java -jar "$originalPath"/DeXTT-Bitcoin-1.0-SNAPSHOT.jar @mint.txt &> mint.log

                            # generate one block on each chain -> balances already usable on startup
                            for i in {1..3}; do
                                btcDir=.bitcoin"$i"
                                datadir="$originalPath""/""$directoryName""/""$btcDir"
                                bitcoin-cli -datadir="$datadir" generatetoaddress 1 "$GENADDR" &>/dev/null
                            done
                            
                            java -jar "$originalPath"/DeXTT-Bitcoin-1.0-SNAPSHOT.jar @generate.txt &> generate.log &
                        fi
                        
                        for addr in ${btcAddresses[@]}; do
                            filename="regtest-"$addr".txt"
                            touch $filename

                            echo $urls >> $filename
                            echo "--address ""$addr" >> $filename
                            echo "--chain REGTEST evaluationrun -g 1 -m HASHREFERENCE" >> $filename
                            echo "-c caae72556fc36d3a8c859029047119b712717419,ca43f8812da1a4219d91e83a6af49d7dfd7c9a67,3d716c6f95050fc1aacac4ae6b1da4b71bdbfdcc,7bafc011aa9a94f5a2106a8d55d0196796fe5e84,1e915de3c3f543b33d1e290ecf797ca0299eae1e,4ede41dd3b87029025249a27fc04c9f971741c69,f177543aa32d9f579fd21ef72e56004a51565ae1,c708ab808319d9169f3cfbeccaf375e18a6e7794,b61c76a540fa2ad239dbcd4215e43f6aafe6d3bb,6c5cee6ebe9ef4868207ca51b3cfd6b4473f416d" >> $filename
                            echo "--runtime "$runtime" --transactionTime "$txtime >> $filename
                            if [ $unconfirmed == 1 ]; then
                                echo "-u" >> $filename
                                echo "-w "$wait >> $filename
                            else
                                echo "-b "$wait >> $filename
                            fi

                            if [ $veto \> 0 ]; then
                                echo "--allowDoubleSpend --enableAutoUnlocking --forceVetos" >> $filename
                            fi

                            if [ $startprograms == 1 ]; then
                                java -Xms256m -Xmx256m -jar "$originalPath"/DeXTT-Bitcoin-1.0-SNAPSHOT.jar @"$filename" &> "regtest-"$addr".log" &
                            fi
                        done

                        if [ $startprograms == 1 ]; then
                            sleep 1
                        fi
                    else
                        ((configs+=1))
                        echo "Configuration ""$configs"" already existed."
                    fi
                    ((configs+=1))
                    echo "Configuration ""$configs"" finished."
                done
            done
        done
    done
done

echo "Number of configurations: "$configs
echo "Done"


