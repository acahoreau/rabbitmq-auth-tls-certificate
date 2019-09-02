# Description

Ce projet présente un exemple d'identification par certificat via AMQPS auprès d'un serveur RabbitMQ. L'utilisateur est identifié à partir du CN (common name) du certificat client fourni, en l'occurence `zenika`.


# Arborescence

Voici la description des éléments constituant cette démonstration :
* `conf` regroupe :
  * `rabbitmq.config` définissant la configuration du serveur RabbitMQ.
  * `enabled_plugins` définissant les plugins activés (en particulier `rabbitmq_auth_mechanism_ssl`).
* `testca` définit une autorité de certification auto-signée.
* `server` regroupe un certificat issu de `testca` à destination du serveur RabbitMQ, ainsi que les éléments ayant permis sa génération (requête et clef privée).
* `client` regroupe :
  * un certificat issu de `testca` à destination d'un client AMQP, ainsi que les éléments ayant permis sa génération (requête et clef privée).
  * `tls-client.jar` encapsulant une application Java se connectant à l'URL `amqps://localhost:5671/%2F` à l'aide du certificat `client_certificate.p12` présent dans le répertoire courant.
  * `java` contenant les sources de l'application `tls-client.jar`
* `definitions.json` définit des ressources par défaut créées au démarrage du serveur RabbitMQ (e.g. des utilisateurs).
* `docker-compose.yml` définit l'usage d'une image Docker `rabbitmq` officielle configurée pour gérer l'identification par certificat.
* `Dockerfile` définit une image spécifiquement configurée pour gérer l'identification par certificat (à partir de l'image Docker `rabbitmq` officielle).

Les différents certificats ont été générés suivant la même procédure que celle présentée dans la documentation : https://www.rabbitmq.com/ssl.html#manual-certificate-generation


# Démarrage d'un serveur RabbitMQ

Pour simplifier l'installation et le démarrage, il est proposé d'utiliser l'image Docker `rabbitmq` officielle en la personnalisant de manière adéquate. Cependant, il est aussi possible de configurer une installation locale d'un serveur RabbitMQ à partir des fichiers des répertoires `conf`, `server` et `testca`.

Un serveur RabbitMQ peut donc être démarré :
* soit en configurant l'image Docker officielle `rabbitmq` :
	```
	docker-compose -f docker-compose.yml up
	```
* soit en créant une image Docker personnalisée à partir de l'image officielle `rabbitmq` :
	```
	docker build -f Dockerfile -t rabbit-auth-tls .
	docker run --rm -p 5671:5671 -p 15671:15671 rabbit-auth-tls
	```

Dans les 2 cas, l'accès AMQPS est exposé sur le port 5671 et l'accès à l'interface d'administration est exposée sur le port 15671.  
Notre autorité de certification étant auto-signée, un avertissement (type `SEC_ERROR_UNKNOWN_ISSUER`) peut être observé lors de l'accès à l'UI https://localhost:15671/ : il est alors nécessaire d'indiquer ce certificat comme étant de confiance.

Le fichier `conf/rabbitmq.config` précise l'emplacement des autres dépendances nécessaires au serveur :
* le certificat CA
* le certificat serveur (issu de la CA)
* la clef privée du serveur
* le fichier `definitions.json`
Seul le fichier `conf/enabled_plugins` n'est pas référencé par cette configuration et doit être placé dans le même répertoire que le fichier `rabbitmq.config`.  
Les arborescences de ces fichiers indiquées dans le fichier `conf/rabbitmq.config` correspondent à celles de l'image Docker officielle `rabbitmq`. Ces arborescences doivent être adaptées à l'emplacement exact de ces fichiers dans le cas d'une installation locale.



# Démarrage du client AMQPS

Le client AMQPS fourni peut être démarré ainsi :
```
cd client
java -jar tls-client.jar
```
Cette application nécessite Java 8 (ou version supérieure) pour être exécutée.

Si le client parvient à s'identifier via son certificate `client_certificate.p12` et échanger un message avec un serveur RabbitMQ localisé `amqps://localhost:5671/%2F`, la dernière trace de la sortie standard devrait afficher `Received: Hello, TLS World!`.

Si l'on souhaite utiliser un autre certificat PKCS#12 que celui fourni, la passphrase du certificat peut être précisée en argument de l'application Java :
```
java -jar tls-client.jar MySecretPassword
```
Si cet argument n'est pas précisé, la valeur par défaut `MySecretPassword` est utilisée.


Au besoin, la classe `client/java/src/main/java/com/zenika/rabbitmq/TlsClient.java` peut être modifiée. Il faut alors recompiler l'application Java :
* Sous Linux
	```
	cd client/java
	./mvnw clean package
	```
* Sous Windows
	```
	cd client/java
	mvnw.cmd clean package
	```
Le nouveau `tls-client.jar` alors produit est situé dans le répertoire `client/java/target`. Cette archive `tls-client.jar` doit ensuite être placée au même niveau (i.e. même répertoire) que le certificat client `client_certificate.p12`.

