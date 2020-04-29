#!/bin/bash

set -e # exit if a command fails
GENADDR="bcrt1qyy44rea9a79y8y0fhhnttvype6wcz40gpqgg0h"

if [ "$1" == "" ]; then 
    echo "Bitcoin datadir missing, usage: ""$0"" <datadir>"
    exit 1
fi

echo "Starting bitcoin setup..."

# private keys of clients (WIF)
declare -a privateKeys=("cRCFqu3zcDHZoSqWdwhwgEj3gCLSbEtLMp2TzJudebnSKvP7Shvn" "cMbQWoFkDbhWZ2H2SCAYLrY59Uvh3NnJUndoCXa79ak5qEfTfKsb" "cQsXpX7DNCaZZPjGT5uFgEDyqCJqZfra71RxaW2MrpcLXU2stStj" "cVoYg961JiX1QPWyNufCTuap9gMMk17ng1PvH8mjuYZogYwEfDoe" "cPo4iKs4pXCBvzPNcB1ScVLZ8JT5c2XDvWLE2m6fkggP72fHjfP5" "cQSCU5ZB5mY6dc12fscZT63kMV3P3wWsusaVUQWezdzm13UJQKRE" "cVjBsEwRMZL7jzF2H48eFemgVKkLLJbvhgaJq4pmdZywGD7cnAev" "cNhP74KMB38r5mRhhxG7YAbLj8xF7pevH29axbuYAXNCk5kKbut6" "cRdwxFnfnGvxEBL8KaVFjaWsJG155BFTsigm8PFJiV1dbxTf594d" "cSqxdBkowvm43vAhcyTLajWtpLSCvdtsryB6agYndA9HFRxANxnk")
# btc addresses of clients
declare -a btcAddresses=("bcrt1qejv7wf8ax4lkag9w3hclyzu47zgj7q6eqv6s60" "bcrt1q30t4dep9laf3k8ys84xrx752awwuzt97gsvc3d" "bcrt1qzuey56rpj0eef2pjc4uz2hhwf30vkk83v2z433" "bcrt1q0gml6gq8q9ev80pqutqst25szpzgw7gsp72eke" "bcrt1qhwu5jng39tfehrxtjuhwc5wnv5n9sxrn0rc095" "bcrt1qjftzzcn2avx2t7lypjf5dpheu0pw08vse3vmlk" "bcrt1qvqcn4676wplcz8d73t8dgvxy8v269h4ra467lj" "bcrt1qt9w03gaxxcw9pnan4wx4enppxxlznvmuaranxx" "bcrt1qpa2vwlw60xp07zvs89hs3xeff6xshv6sp00f6s" "bcrt1qu3sdjufevlnm7wuld5a7qague5mcwhkdz3swfr")

echo "Importing private keys..."
for key in ${privateKeys[@]}; do
   #echo "$key"
   bitcoin-cli -datadir="$1" importprivkey "$key" "dexxt"
done
echo "Done importing private keys."

echo "Generating to addresses..."
for addr in ${btcAddresses[@]}; do
    bitcoin-cli -datadir="$1" generatetoaddress 50 "$addr"
done
echo "Done generating to addresses."

echo "Generating blocks to unlock funds..."
bitcoin-cli -datadir="$1" generatetoaddress 101 "$GENADDR"
echo "Done generating blocks."

echo "Setup done."