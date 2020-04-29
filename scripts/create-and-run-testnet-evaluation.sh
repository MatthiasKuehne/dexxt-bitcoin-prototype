#!/bin/bash
set -e # exit if a command fails

declare -a btcAddresses=("tb1qejv7wf8ax4lkag9w3hclyzu47zgj7q6ez9radx" "tb1q30t4dep9laf3k8ys84xrx752awwuzt972e44xy" "tb1qzuey56rpj0eef2pjc4uz2hhwf30vkk83wrmcxc" "tb1q0gml6gq8q9ev80pqutqst25szpzgw7gsrhn5ps" "tb1qhwu5jng39tfehrxtjuhwc5wnv5n9sxrnd2pzja" "tb1qjftzzcn2avx2t7lypjf5dpheu0pw08vsmc4kgl" "tb1qvqcn4676wplcz8d73t8dgvxy8v269h4rlurngm" "tb1qt9w03gaxxcw9pnan4wx4enppxxlznvmul2y730" "tb1qpa2vwlw60xp07zvs89hs3xeff6xshv6srxkyde" "tb1qu3sdjufevlnm7wuld5a7qague5mcwhkdqcfr72")


echo "Starting..."
startprograms=0
originalPath=$(pwd)
runtime=83077
genesisblock=1669373

unconfirmed=1
txtime=6468 # double the maximum value for 10 min blocks, as testnet has very likely blocks at 20 mins interval
wait=10000 # actual the default, wait 10 seconds in unconfirmed mode
veto=0

port=18332
urls="--urlrpc http://matthias:password@127.0.0.1:""$port""/"

directoryName="evaldata/testnetrun"

if [ ! -d "$directoryName" ]; then
    mkdir -p "$directoryName"
    cd "$directoryName"

    dexxtconfig="configuration.csv"
    touch $dexxtconfig
    echo "unconfirmed,wait,txtime,veto" >> $dexxtconfig
    echo $unconfirmed","$wait","$txtime","$veto >> $dexxtconfig


    for addr in ${btcAddresses[@]}; do
        filename="regtest-"$addr".txt"
        touch $filename

        echo $urls >> $filename
        echo "--address ""$addr" >> $filename
        echo "--chain TESTNET evaluationrun -g $genesisblock -m HASHREFERENCE" >> $filename
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
fi

echo "Started instances."