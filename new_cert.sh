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
	#Creation of the CSR
	sudo openssl req -new -nodes -config /etc/ssl/openssl.cnf -newkey ec:../private/reference_key.key -keyout private/$nom.key -out certs/$nom.req #Creation of the certificate request server
	#Signature by the CA
	sudo openssl ca -config /etc/ssl/openssl.cnf -extensions SERVEUR -name serveur_ca -in certs/$nom.req -out newcerts/$nom.pem -notext #Signature of the certificate request server

	sudo rm certs/$nom.req
	
	# "req" Use to create and process certifcate request
	#-config Specification of the configuration file
	#-newkey Create a new key with the follow parameter (For everything that is not a RSA key you need a reference file, here we use another key.)
	#-keyout Out file of the new key
	#-out Specify the name and the location of the new certificate request
	
	#"ca" Use for sign certificate request
	#-config Specification of the configuration file
	#-extensons Specify the extensions to use in the config file
	#-in Specify the localisation of certificate request to be signed
	#-out Specify the name and the location of the new CA


else

	cd /etc/ssl/client_ca
	#Creation of the CSR
	sudo openssl req -new -nodes -config /etc/ssl/openssl.cnf -newkey ec:../private/reference_key.key -keyout private/$nom.key -out certs/$nom.req #Creation of the certificate request client	
	#Signature by the CA
	sudo openssl ca -config /etc/ssl/openssl.cnf -extensions CLIENT -name client_ca -in certs/$nom.req -out newcerts/$nom.pem -notext #Signature of the certificate request client


	sudo rm certs/$nom.req
	
	# "req" Use to create and process certifcate request
	#-config Specification of the configuration file
	#-newkey Create a new key with the follow parameter (For everything that is not a RSA key you need a reference file, here we use another key.)
	#-keyout Out file of the new key
	#-out Specify the name and the location of the new certificate request
	
	#"ca" Use for sign certificate request
	#-config Specification of the configuration file
	#-extensons Specify the extensions to use in the config file
	#-in Specify the localisation of certificate request to be signed
	#-out Specify the name and the location of the new CA
fi
