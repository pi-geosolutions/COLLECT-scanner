# COLLECT-scanner

## Présentation

COLLECT-scanner est une application développée dans le cadre de la fonctionnalité de collecte de données via owncloud, du projet PADRE.

Dans le contexte du projet PADRE (pays à faible connectivité internet), les agents chargés de la collecte et de la saisie de données régulières (données météo, prix des marchés, etc) ne disposent que rarement d’une bonne connexion internet. Un outil de saisie en ligne est donc à proscrire, dans la mesure du possible, et du moins en ce qui concerne les actions quotidiennes.

Une solution s’appuyant sur l’application open-source Owncloud, permettant de synchroniser en tâche de fond des dossier avec un serveur, est en cours de développement.

COLLECT-scanner est un des composants de cette solution. 

**Prérequis :** Cet outil nécessite Java 7 ou ultérieur.

## Fonctionnalités

### Scan

COLLECT-scanner a pour fonction de scanner un dossier et son contenu (de façon récursive), à la recherche de fichiers précis.

Le pattern de nommage de ces fichiers (nom et chemin des fichiers) est défini en paramètres de l’application, ce qui permet de ne traiter que certains fichiers du dossier.

COLLECT-scanner est ainsi capable de scanner le contenu du dossier de données Owncloud (à condition que l’instance Owncloud, sur le serveur, n’encrypte pas ses données. Cf.  la configuration de l’instance owncloud) mais aussi de n’importe quel autre dossier, à condition que l’utilisateur exécutant COLLECT-scanner ait les droits en lecture/écriture sur ce dossier et son contenu.

### Publication

Les fichiers scannés sont publiés, un par un (l’ordre de scan n’est pas fixe), dans la base de données configurée dans le fichier de propriétés (voir Configuration).

En cas d’erreur lors du traitement d’un fichier, aucune donnée de ce fichier n’est publiée, une erreur est rapportée (voir Logs), et le fichier est maintenu en place.

Si le traitement du fichier se passe bien, les données sont publiées dans la base et le fichier est archivé (supprimé, renommé ou archivé, selon la configuration. Voir Configuration).

*Attention : COLLECT-scanner est conçu pour publier les données dans une table (de base de donnée) existante. Il ne créera pas la table en cas de besoin.*

## Conventions

Comme tout outil automatisant au maximum la tâche à réaliser, COLLECT-scanner repose sur un bon nombre de conventions à respecter (un certain nombre d’entre elles peuvent cependant être modifiées dans la configuration).

### Formats supportés

**En entrée**

Pour l’instant, seul le format CSV est supporté.

**En sortie**

Le choix de la base de données destination est, en théorie, ouvert. Cependant, l’outil n’a été testé qu’avec une base postgresql. Il est possible que des ajustements mineurs soient nécessaires avec un autre SGBD.

### Conventions de nommage

**Nom du fichier**

Le fichier doit avoir l’extension .csv

Son nom doit commencer par le nom de la table de destination, suivi de "--". Le reste n’a pas d’importance.

Exemple de fichier valide : 

`meteo_pluiesquot--user2--160222_1228.csv`

 (à publier dans la table c_meteo_pluiesquot. Voir Nom de la table, pour l’explication du préfixe c_).

**Chemin du fichier**

Par défaut, le scan des fichiers est restreint par un pattern de chemin d’accès (permet d’éviter de scanner les fichiers et dossiers utilisés en interne par owncloud, par exemple).

Ce pattern est défini dans le fichier de configuration et peut être modifié.

Par défaut, il est défini à `**/files/collect/*.csv` c’est à dire qu’il cherchera les fichiers CSV (extension .csv) dans les dossiers collect, placés à la racine des dossiers utilisateurs d’owncloud. Où que soient ces dossiers files eux-même. Le reste des fichiers sera ignoré.

**Nom de la table**

Le nom de la table doit correspondre au nom des fichiers à publier. A la différence près d’un préfixe : afin de pouvoir identifier rapidement, dans la base de données, les tables utilisées par la collecte, nous préfixerons par défaut chaque table par "c_".

Pour la collecte des données pluvio météo, par exemple, nous créerons donc une table qui s’appellera c_meteo_pluiequot. Et les fichiers CSV correspondants devront donc suivre le pattern meteo_pluiesquot--*.csv

*Note : le préfixe peut être changé (voire supprimé) via le fichier de configuration*

## Mise en place

### Création de la table

Par défaut, un schema spécial est utilisé, appelé collect. Si vous voulez utiliser le schema par défaut, ajuster la configuration.

La table doit être créée dans la base de donnée, à la main (en utilisant un outil comme phppgadmin, par exemple, ou en ligne de commande).

*Attention : le format de fichiers CSV ne permet pas de typer les données. L’outil n’a donc aucun moyen de savoir de quel type (chaîne de caractères, entier, réel, date) est chaque donnée, si ce n’est via la définition de la table. ***_C’est donc la table qui définit le modèle de données. Et les fichiers qui doivent suivre ce modèle._**

Il est également possible de définir des clefs primaires et étrangères sur la table. Les données à publier (dans les fichiers CSV) devront respecter ces contraintes, sous peine d’échec de la publication.

### Mise en place de l’instance owncloud

N’entre pas dans le cadre de cette doc.

Cependant, afin de respecter la convention de base de la collecte, il conviendra de créer un dossier "collect" dans le compte des utilisateurs voulant publier leurs fichiers CSV.

Les fichiers CSV devront être déposés dans ce fichier.

### Personnalisation de la configuration

Ne pas oublier d’ajuster la config. Voir Configuration.

### Exécution

Le fichier est un fichier jar (java, application spring-boot) et s’exécute de la façon standard. Ne pas oublier d’appeler le fichier de configuration personnalisé dans la commande : 

`java -jar COLLECT-scanner-0.0.1-SNAPSHOT.jar --spring.config.location=file:config/application.properties`

### Exécution automatique : tâche CRON

Le scan est supposé se faire à intervalles réguliers, Il est donc conseillé de l’exécuter dans le contexte d’une tâche cron ou équivalent. Il suffit de lancer, dans le CRON, la commande ci-dessus, exécutée par un utilisateur ayant les droits suffisants (lecture+écriture) sur l’arborescence de fichiers.

## Configuration

Ci-dessous, le paramètre est en gras, une `valeur exemple` du paramètre suit. En-dessous de chaque paramètre de config, l’explication.

**dir.path**=`/home/pigeo/domains/sn-risk.pigeo.fr/data/oc/data`

Le chemin racine à scanner. A priori, le chemin "data" de l’instance owncloud à exploiter.

**file.pattern**=`**/files/collect/*.csv`

Le schema (pattern) de chemin d’accès. Définit à la fois le chemin d’accès et le schema du nom de fichier. Suit le modèle définit pour Maven [DirectoryScanner](https://maven.apache.org/shared/maven-shared-utils/apidocs/org/apache/maven/shared/utils/io/DirectoryScanner.html)

**file.partsSeparator**=`--`

Permet de découper le nom du fichier et extraire le nom de la table à cibler.

*Ex. : meteo_pluiesquot--user1--160222_1228.csv -> meteo_pluiesquot*

Ce qui suit la première occurrence de "--" n’est pas utilisé.

Si on change ce paramètre, on peut changer de séparateur. 

**file.postPublishPolicy**=`delete`

Accepte 'delete', 'rename' or 'archive'.

La démarche à suivre lorsqu’un fichier a été publié avec succès. Habituellement, on voudra le supprimer (‘delete’), le renommer (‘rename’) ou l’archiver dans un dossier (‘archive’). Toute autre valeur fera que rien ne se passera. Le fichier sera donc conservé au même endroit, au risque de générer des erreurs au prochain scan (l’update des données n’est pas implémenté)
#used if postPublishPolicy is set to 'archive'

**file.archiveDirectory**=`/home/pigeo/domains/sn-risk.pigeo.fr/data/oc/oc_collect_archives`

Dossier utilisé pour l’archivage si on a choisi ‘archive’

**file.renameExtension**=`.done`

Extension rajoutée au fichier si on a choisi ‘rename’

**csv.separator**=`;`

Séparateur des champs, dans le fichier CSV. Habituellement, ‘,’ ou ‘;’ ou ‘\t’ (tabulation)

**csv.quotechar**=`"`

Guillemets utilisés pour encadre une chaine de caractères. Probablement pas besoin d’y toucher.

**csv.skiplines**=`0`

Nombre de lignes en début de fichier à sauter. Par exemple s’il y avait des commentaires. A priori, laisser tel quel (on a besoin de lire les noms de champs)

**csv.ignoreFields**=`exported`

Ne pas publier certains champs (ici, la colonne exported)

**parsing.dateformat**=`dd/MM/yyyy HH:mm:ss`

Définit le format de date utilisé. Utiliser un format cohérent pour l’ensemble des données à collecter !

**parsing.locale**=`fr-FR`

La locale utilisée. Sert pour la lecture des données numérique notamment : dans la locale fr-FR, un réel s’écrira 17,53 alors que dans la locale en-US, par exemple, le même réel s’écrira 17.53.

**jdbc.driver**=`org.postgresql.Driver`

**jdbc.url**=`jdbc:postgresql://localhost:5432/sn_risk_geodata

**jdbc.user**=`collect`

**jdbc.password**=`collect`

Configuration JDBC (connexion à la base de données)

**db.schema**=`collect`

Schema dans lequel on cherche les tables.

**db.collectTablePrefix**=`c_`

Préfixe utilisé pour la correspondance noms de fichier->table

Ex. : fichier meteo_pluiesquot--user1--160222_1228.csv -> table **c_**meteo_pluiesquot


**logging.file**=`/home/jean//logs/collect/scanner/scanner.log`

Emplacement du fichier de log. S’assurer que l’emplacement est accessible en écriture par l’utilisateur exécutant le code (a priori, www-data, propriétaire du dossier de données owncloud)

## Logs

A faire...

## Exemples

### Correspondance tables/fichiers
```
CREATE TABLE collect.c_meteo_stations

(

  "IdStation" bigint NOT NULL, -- ID (not serial since the values will be set from an Access Database, ie external checks on the sequence.)

  "NomStation" character varying(100),

  "LonStation" double precision,

  "LatStation" double precision,

  CONSTRAINT id PRIMARY KEY ("IdStation")

)

WITH (

  OIDS=FALSE

);

ALTER TABLE collect.c_meteo_stations

  OWNER TO collect;

COMMENT ON TABLE collect.c_meteo_stations

  IS 'Stations météo (collecte pluies quotidiennes)';

COMMENT ON COLUMN collect.c_meteo_stations."IdStation" IS 'ID (not serial since the values will be set from an Access Database, ie external checks on the sequence.)';
```
définit la table qui pourra recevoir les données de fichiers tels que celui-ci : 

`meteo/user1/files/collect/meteo_stations--user1--160222_1228.csv` : 
```
"IdStation";"NomStation";"LonStation";"LatStation"
3;"dakar gare";-17,58;57,25
```
De même,

```
CREATE TABLE collect.c_meteo_pluiesquot

(

  "IdMesure" integer NOT NULL,

  "CodeStation" integer NOT NULL,

  "DateMesure" date NOT NULL,

  "Pluiemm" integer NOT NULL,

  CONSTRAINT "Id" PRIMARY KEY ("IdMesure"),

  CONSTRAINT key_foreign_codestation FOREIGN KEY ("CodeStation")

      REFERENCES collect.meteo_stations ("IdStation") MATCH SIMPLE

      ON UPDATE NO ACTION ON DELETE NO ACTION

)

WITH (

  OIDS=FALSE

);

ALTER TABLE collect.c_meteo_pluiesquot

  OWNER TO collect;

COMMENT ON TABLE collect.c_meteo_pluiesquot

  IS 'Collected data about rainfalls (links with meteo_stations)';
```
définit la table qui pourra recevoir les données de fichiers tels que celui-ci : 

`meteo/user1/files/collect/meteo_pluiesquot--user1--160222_1228.csv` 

```
"IdMesure";"CodeStation";"DateMesure";"Pluiemm";"exported"
1;2;3/2/2016 00:00:00;10;0
2;2;4/2/2016 00:00:00;25;0
3;2;5/2/2016 00:00:00;5;0
4;2;15/2/2016 00:00:00;5;0
5;2;1/2/2016 00:00:00;85;0
```

*Remarque : dans sa définition, cette table définit une clef étrangère sur la table c_meteo_stations définie ci-dessus).*

*Chaque ligne du fichier CSV devra fournir un code CodeStation valide (i.e. déjà présent dans la table c_meteo_stations au moment de sa publication).*

### Fichier de configuration pour le sénégal
```
#Spring boot base config
spring.main.web_environment=false
spring.main.banner_mode=log

#Input config
dir.path=/home/pigeo/domains/sn-risk.pigeo.fr/data/oc/data
file.pattern=**/files/collect/*.csv
file.partsSeparator=--
#Accepts 'delete', 'rename' or 'archive'
#file.postPublishPolicy=archive
file.postPublishPolicy=delete
#used if postPublishPolicy is set to 'archive'
file.archiveDirectory=/home/pigeo/domains/sn-risk.pigeo.fr/data/oc/oc_collect_archives
#used if postPublishPolicy is set to 'rename'
file.renameExtension=.done
csv.separator=;
csv.quotechar="
csv.skiplines=0
csv.ignoreFields=exported

#Parsing
parsing.dateformat=dd/MM/yyyy HH:mm:ss
parsing.locale=fr-FR

#Output config
jdbc.driver=org.postgresql.Driver
jdbc.url=jdbc:postgresql://localhost:5432/sn_risk_geodata
jdbc.user=collect
jdbc.password=collect
db.schema=collect
db.collectTablePrefix=c_

#Logging

logging.file=/home/jean//logs/collect/scanner/scanner.log
```
