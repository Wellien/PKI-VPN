#!/bin/bash
echo "Create your own PKI with Elliptic Curve KEY."


cd /etc/ssl

echo "Creation of the key reference"

sudo openssl ecparam -name secp384r1 -genkey -out private/reference_key.key #Creation de la clé de référence pour les différente clé des CA
############################################################################

#Configuration openssl.cnf
if [ -e openssl.cnf ];then
sudo mv openssl.cnf openssl.cnf.old #Save the old configuration file
fi
sudo wget https://raw.githubusercontent.com/Wellien/PKI-VPN/master/openssl.cnf


############################################################################

##Create root_ca

sudo mkdir root_ca  #Create the main directory

cd ./root_ca

sudo mkdir certs newcerts index serial private # Creation of all the directory

touch index/index.txt index/index.txt.attr # Creation of the "Data_Base of the CA"

touch serial/serial # Creation of the serial

# Creation of the ROOT Authority Certification 

sudo openssl req -x509 -config /etc/ssl/openssl.cnf -newkey ec:/etc/ssl/private/reference_key.key -keyout private/root_ca.key -extensions ROOT_CA -days 7300 -out certs/root_ca.pem -subj "/C=FR/ST=Auvergne-Rhone-Alpes/L=Annecy/O=IUT/OU=R&T/CN=PKI&VPN_ROOT_CA/emailAddress=pkivpn.project@gmail.com"

# "req" Use for create and process certifcate request
# -x509 Use auto-sign
#-config Specification of the configuration file
#-newkey Create a new key with the follow parameter ( For a not RSA key you need a file referencen, here another key
#-keyout Out file of the new key
#-extensons Specify the extensions to use in the config file
#-days Time to live of the CA
#-out Specify the name and the location of the new CA

sudo echo 1000 >serial/serial # INITIALISATION DU SERIAL


############################################################################

##Create Client_CA

echo "Creation of CLIENT_CA"
cd /etc/ssl/

sudo mkdir client_ca #Create the main directory

cd client_ca

sudo mkdir certs newcerts index serial private # Creation of all the directory

touch index/index.txt index/index.txt.attr # Creation of the "Data_Base of the CA"

touch serial/serial # Creation of the serial

## Creation of the CLIENT CERTFICATE REQUEST

sudo openssl req -new -config ../openssl.cnf -newkey ec:../private/reference_key.key -keyout private/client_ca.key -out certs/client_ca.req -subj "/C=FR/ST=Auvergne-Rhone-Alpes/L=Annecy/O=IUT/OU=R&T/CN=PKI&VPN_CLIENT_CA/emailAddress=pkivpn.project@gmail.com"

# "req" Use for create and process certifcate request
#-config Specification of the configuration file
#-newkey Create a new key with the follow parameter ( For a not RSA key you need a file referencen, here another key
#-keyout Out file of the new key
#-out Specify the name and the location of the new certificate request

## Sign the CLIENT CERTIFICATE REQUEST

sudo openssl ca -extensions CLIENT_CA -in certs/client_ca.req -out certs/client_ca.pem -batch -notext

#"ca" Use for sign certificate request
#-extensons Specify the extensions to use in the config file
#-in Specify the localisation of certificate request to be signed
#-out Specify the name and the location of the new CA
#-notext Don't show the text form of a certificate in the output file.

sudo rm certs/client_ca.req

sudo echo 1000 >serial/serial # INITIALISATION DU SERIAL


##############################################################################

##Create Server_CA

echo "Creation of Server_CA"
cd /etc/ssl/

sudo mkdir serveur_ca #Create the main directory

cd serveur_ca

sudo mkdir certs newcerts index serial private # Creation of all the directory

touch index/index.txt index/index.txt.attr # Creation of the "Data_Base of the CA"

touch serial/serial # Creation of the serial

## Creation of the SERVER CERTFICATE REQUEST

sudo openssl req -new -config ../openssl.cnf -newkey ec:../private/reference_key.key -keyout private/serveur_ca.key -out certs/serveur_ca.req -subj "/C=FR/ST=Auvergne-Rhone-Alpes/L=Annecy/O=IUT/OU=R&T/CN=PKI&VPN_SERVER_CA/emailAddress=pkivpn.project@gmail.com"

# "req" Use for create and process certifcate request
#-config Specification of the configuration file
#-newkey Create a new key with the follow parameter ( For a not RSA key you need a file referencen, here another key
#-keyout Out file of the new key
#-out Specify the name and the location of the new certificate request

## Sign the SERVER CERTIFICATE REQUEST

sudo openssl ca -extensions SERVEUR_CA -in certs/serveur_ca.req -out certs/serveur_ca.pem -batch -notext

#"ca" Use for sign certificate request
#-extensons Specify the extensions to use in the config file
#-in Specify the localisation of certificate request to be signed
#-out Specify the name and the location of the new CA
#-notext Don't show the text form of a certificate in the output file.

sudo rm certs/serveur_ca.req

sudo echo 1000 >serial/serial # INITIALISATION DU SERIAL




