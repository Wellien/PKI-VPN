#!/bin/bash

filename=$1
email=$2
test=test

cd /home/www-java/
spkac_file="certs_req/"$filename".spkac"
echo "Spkac file : $spkac_file" >> out.log
openssl ca -config ca/openssl.cnf -spkac $spkac_file -keyfile client_ca/private/client_ca.key  -cert client_ca/client_ca.pem -status

cert="client_ca/newcrt/"$(cat client_ca/serial.old)".pem"
echo "cert : $cert" >> out.log
cat $cert |mail -s "Retour de votre clé machin" $2


