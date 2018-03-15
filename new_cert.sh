#!/bin/bash

askYesNo() {
		read -p "$1 [server/client]" choice
		case "$choice" in
			server|SERVER|Server ) return 0;;
	   		client|CLINET|CLIENT ) return 1;;
			* ) return $(askYesNo "$1");;
		esac
}

echo "Nom du certificat ?" 
read "nom"

if askYesNo "Do you want a server certificate or client certificate ?";
then

	cd /etc/ssl/serveur_ca

	sudo openssl req -new -config /etc/ssl/openssl.cnf -newkey ec:../private/reference_key.key -keyout private/$nom.key -out certs/$nom.req #Creation of the certificate request server
	
	sudo openssl ca -config /etc/ssl/openssl.cnf -extensions SERVEUR -name serveur_ca -in certs/$nom.req -out newcerts/$nom.pem -notext #Signature of the certificate request server

	sudo rm certs/$nom.req

else

	cd /etc/ssl/client_ca
	
	sudo openssl req -new -config /etc/ssl/openssl.cnf -newkey ec:../private/reference_key.key -keyout private/$nom.key -out certs/$nom.req #Creation of the certificate request client	

	sudo openssl ca -config /etc/ssl/openssl.cnf -extensions CLIENT -name client_ca -in certs/$nom.req -out newcerts/$nom.pem -notext #Signature of the certificate request client


	sudo rm certs/$nom.req
fi
