#!/bin/bash

cd /home/www-java/
filename=$1
email=$2
echo $(date)" - Received request with $1 $2" >> log.txt
spkac_file="/home/www-java/certs_req/"$filename".csr"

echo "Spkac file : $spkac_file" >> log.txt

output=$(openssl ca -config /etc/ssl/openssl.cnf -name client_ca -spkac $spkac_file -keyfile /home/www-java/client_ca/private/client_ca.key  -cert /home/www-java/client_ca/client_ca.pem -batch)

cert="/home/www-java/client_ca/newcrt/"$(cat client_ca/serial.old)".pem"
echo $(date)" Generated cert $cert successfully"  >> log.txt

#Todo send back signed key via the mail command
#cat $cert |mail -s "Retour de votre clé" $email


