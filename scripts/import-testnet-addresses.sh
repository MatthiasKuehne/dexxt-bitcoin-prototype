#!/bin/bash
set -e # exit if a command fails

declare -a privateKeys=("cRCFqu3zcDHZoSqWdwhwgEj3gCLSbEtLMp2TzJudebnSKvP7Shvn" "cMbQWoFkDbhWZ2H2SCAYLrY59Uvh3NnJUndoCXa79ak5qEfTfKsb" "cQsXpX7DNCaZZPjGT5uFgEDyqCJqZfra71RxaW2MrpcLXU2stStj" "cVoYg961JiX1QPWyNufCTuap9gMMk17ng1PvH8mjuYZogYwEfDoe" "cPo4iKs4pXCBvzPNcB1ScVLZ8JT5c2XDvWLE2m6fkggP72fHjfP5" "cQSCU5ZB5mY6dc12fscZT63kMV3P3wWsusaVUQWezdzm13UJQKRE" "cVjBsEwRMZL7jzF2H48eFemgVKkLLJbvhgaJq4pmdZywGD7cnAev" "cNhP74KMB38r5mRhhxG7YAbLj8xF7pevH29axbuYAXNCk5kKbut6" "cRdwxFnfnGvxEBL8KaVFjaWsJG155BFTsigm8PFJiV1dbxTf594d" "cSqxdBkowvm43vAhcyTLajWtpLSCvdtsryB6agYndA9HFRxANxnk")

#declare -a btcAddresses=("tb1qejv7wf8ax4lkag9w3hclyzu47zgj7q6ez9radx" "tb1q30t4dep9laf3k8ys84xrx752awwuzt972e44xy" "tb1qzuey56rpj0eef2pjc4uz2hhwf30vkk83wrmcxc" "tb1q0gml6gq8q9ev80pqutqst25szpzgw7gsrhn5ps" "tb1qhwu5jng39tfehrxtjuhwc5wnv5n9sxrnd2pzja" "tb1qjftzzcn2avx2t7lypjf5dpheu0pw08vsmc4kgl" "tb1qvqcn4676wplcz8d73t8dgvxy8v269h4rlurngm" "tb1qt9w03gaxxcw9pnan4wx4enppxxlznvmul2y730" "tb1qpa2vwlw60xp07zvs89hs3xeff6xshv6srxkyde" "tb1qu3sdjufevlnm7wuld5a7qague5mcwhkdqcfr72")


echo "Importing private keys..."
for key in ${privateKeys[@]}; do
   #echo "$key"
   bitcoin-cli importprivkey "$key" "dexxt" false
done
echo "Done importing private keys."

echo "Startint rescan..."

bitcoin-cli rescanblockchain

echo "Done rescanning."


