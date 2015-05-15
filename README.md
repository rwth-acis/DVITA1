#A Web-Based Tool for Building and Exploring Dynamic Topic Models

##Eclipse
Launch or obtain Eclipse IDE for Java Developers. These instructions were made with version 4.3.2 (Kepler), ran with JRE 8 on x86.

Import this repository in Eclipse (either manually or via some plugin). There are two folders/projects: 
* DVITA: the GWT Web App
* OfflineComponents: A set of classes that are needed to process data and construct dynamic topic models suitable for opening in DVITA.

##GWT
If not yet installed in your Eclipse, follow these instructions to install GWT related stuff: https://developers.google.com/eclipse/docs/getting_started

It is sufficient to install the "Google Web Toolkit SDK" and the "Google Plugin for Eclipse".
For Eclipse 4.3:
* Go to Help > Install New Software; add https://dl.google.com/eclipse/plugin/4.3 as a location ("Add" button).
* Select "Google Plugin for Eclipse" and "Google Web Toolkit SDK" (under "SDKs") and finish the wizard

Now import the folder containing all the individual projects in your Eclipse workspace (menu File > Import > General > Existing projects into workspace)

##DVITA Web App - Exploring Topic Models

###Build / Compile
Build the project, if not done automatically.

We need to let GWT Compiler have some fun, too. Right-click project DVITA > Google > GWT compile. Don't change anything, project should be "DVITA", click "Compile"

###Set up DVITA DB
We need to set up a database where DVITA stores app settings and the topic models. Feel free to select any DB2, mysql, or Oracle DBMS in your vicinity (you need credentials with full permissions there).

DVITA needs a config file that defines the connection details of your DB. So create a file `dvita.config` and put the following information there (the example contains fake DB2 database connection details for illustration). 

NOTE 1: `dbtype` can currently be 1 = MySQL, 2 = DB2, 3 = Oracle

NOTE 2: `dbschema` can be omitted when there's no schema, all other fields are mandatory.

```
dbtype=2
dbdriver=com.ibm.db2.jcc.DB2Driver
dbconnection=jdbc:db2://myrottenserver:55005/the_db:currentSchema=topics;
dbschema=topics
dbusername=chucknorris
dbpassword=roundhouse
```

Save this config file in two places:
* in DVITA in the `war/WEB-INF` folder
* in OfflineComponents in the `bin` folder

Initialize the DVITA DB by running `IntitializeConfigTables` (see OfflineComponents below).

###Run/Debug Locally
If there are no build errors: right click project DVITA > Run/Debug As > Web Application.
(Make sure Chrome is your default browser; will ask you to install GWT Developer extension; you will want to accept)

###Deploy Remotely
To generate the WAR file for deployment, right-click the file generateWAR.xml under project DVITA. Select Run As > Ant Build.
Then deploy the generated WAR file in Tomcat.
Then make sure to put a copy of the dvita.config file into the app's WEB-INF directory.

##OfflineComponents - Building Topic Models

###Preparations
On the console go to `OfflineComponents/bin`
* Add the directory DVITA/war/WEB-INF/classes to the CLASSPATH variable
* Add the following JARs in DVITA\war\WEB-INF\lib\ to the CLASSPATH variable: mysql-connector-java-5.1.22-bin.jar, db2jcc.jar, ojdbc6.jar

###Initialize DVITA DB
If you already have an initialized DVITA DB, then move on to the next section.

DVITA can currently fetch the raw documents for the topic model from any DB2, MySQL and Oracle DB. To store these connections and other information it uses a set of configuration tables (prefixed `CONFIG_`) in the DVITA DB. These need to be set up by running the following command:

`java IntializeConfigTables`

This will create the following tables in your DVITA DB:
* CONFIG_CONNECTION: DB connections to lcoal or remote raw data bases, that will be used by the OfflineComponents s to access the raw data when building the topic models. Notable fields:
 * TYPE: Type of DBMS that stores the raw data. 1 = MySQL, 2 = DB2, 3 = Oracle

* CONFIG_RAWDATA: configuration of SQL query template to map raw data in some local or remote DB to offline processing data in the DVITA DB. Each record here uses a particular CONFIG_CONNECTION (referencing the CONFIG_CONNECTION.ID using CONNECTIONID). Notable fields:
 * TABLEPREFIX: a prefix to the names of tables that represent the raw data in the DVITA DB. If your DB imposes restrictions on the length of table names, keep the prefix short.
 * COLUMNNAME{ID,DATE,CONTENT,TITLE,AUTHORS,URL,COPYRIGHT,TEXTDISPLAY} represent the field names in the raw data base that map to these fields. COLUMNNAME{ID, CONTENT, TITLE} are particularly important for the topic mining process. The other fields are relevant for displaying documents to the user in the DVITA web GUI. For example: consider a raw data base PAPERS(ID,PUB_DATE,TITLE,ABSTRACT,BODY,DOI_LINK). If you want to build a topic model based on the paper abstracts, then the mapping should be COLUMNNAMEID = 'ID', COLUMNNAMEDATE = 'PUB_DATE', COLUMNNAMECONTENT = 'ABSTRACT', COLUMNNAMETITLE = 'TITLE', COLUMNNAMEURL = 'DOI_LINK'. COLUMNNAMECOPYRIGHT should point to the field that contains copyright or license text to display in DVITA's document browser (can be NULL).  COLUMNNAMETEXTDISPLAY points to the field that contains the text to display in the document browser. This is handy if you do not want or are not allowed to display the full text, e.g. due to the publisher's license. If NULL, then no text is displayed. COLUMNNAMEAUTHORS can also be NULL.
 * FROM / WHERE: these allow you to provide SQL from and where clauses on the raw data. In the example above one could restrict the model to consider only papers published after 2005 by setting FROM='PAPERS' and WHERE='PUB_YEAR > 2005'
 * CONNECTIONID: references the CONFIG_CONNECTION.ID

* CONFIG_TOPICMINING: info for offline processing. For each CONFIG_RAWDATA record there can be multiple topic models, e.g. with varying numbers of topics and time slice granularities. Notable fields:
 * RAWDATAID: references the CONFIG_RAWDATA.ID from which to build the topic model
 * META{TITLE,DESCRIPTION} will be displayed to the user in the DVITA data set selection dialog, so choose meaningful texts there.
 * GRANULARITY: time slice length { 1 = yearly, 2 = monthly, 3 = weekly, 4 = daily, 5 = quarteryearly, 6 = halfyearly, 7 = five years, 8 = ten years }
 * RANGESTART, RANGEEND will be applied as filters to the DATE column in the CONFIG_RAWDATA record. Both need to be provided
 * TABLEPREFIX will be the prefix of tables that represent the topic model in the DVITA DB. Use short prefixes.
 * NUMBEROFTOPICS: The Dynamic LDA algorithm requires a pre-defined number of topics in the topic model. Note that the number of topics has a significant effect on the runtime of LDA.

* CONFIG_GUI: structures the analyses in the dataset selection dialog in D-VITA. Notable fields:
 * TITLE, DESCRIPTION are displayed to the user in the data set selection dialog.
 * MININGIDS: a space separated list of identifiers, each referencing a TOPICMINING.ID to offer for exploration.

###Build the Topic Model
Execute the following steps sequentially.

###1. Configure the Topic Model
Follow these steps:
* Reuse an existing or insert a new CONFIG_CONNECTION record to point to the raw data DB. 
* Reuse an existing or insert a new CONFIG_RAWDATA record using this connection to allow the offline components to obtain the list of "documents"
* Insert a new CONFIG_TOPICMINING record with all information pertaining to a particular topic model of the raw data. We build topic models using LDA, so each topic model has a predefined number of topics, date range, etc.

Note it is important that the raw data exposes in each time slice at least one document. Otherwise you will see an exception.

###2. Preprocessing
This needs to be performed only once for each RAWDATA record. It can also be performed when the raw data was updated since the last time preprocessing.

The preprocessor will boil the raw data text sources down to plain word bags, apply stopwords (currently English only), and remove short words. It will also store document word distributions in the DB.

Command template:
`java Preprocessing RAWDATA_ID`

* RAWDATA_ID is the actual ID from CONFIG_RAWDATA.ID
* In the D-VITA database there are now 2 tables: *X*_CONTAINS and *X*_WORDS, with *X* being the configured table prefix for the raw data (CONFIG_RAWDATA.TABLEPREFIX)

Example: `java Preprocessing 14`

###3. Run Dynamic LDA
This will run [David Blei's LDA-based Dynamic Topic Model](http://en.wikipedia.org/wiki/Dynamic_topic_model) algorithm to build a model of the topic dynamics in the data set. The algorithm is implemented in the [dtm package](https://github.com/Blei-Lab/dtm). You will have to compile the dtm code for your machine.

Command template:
`java DynamicLDA TOPICMINING_ID DLDA_PATH`

* TOPICMINING_ID is the actual ID from CONFIG_TOPICMINING.ID
* DLDA_PATH is the absolute path to the directory of the main executable of Blei's dtm package. 
* Watch the console output, and see whether the identified time windows (and number of time windows) cover the full range of raw data dates. If not, CONFIG_TOPICMINING.{RANGESTART, RANGEEND} need adjustment. For yearly models, the range should be *XXXX*-01-01 to *YYYY*-01-01 with *XXXX* being the first year of interest, and *YYYY* being the year after (!) the last year of interest.

Example: `java DynamicLDA 40 C:\XYZ\DLDA`

###4. Topic Ranking
The DVITA web app will offer various ways of sorting the topic list (see [this paper](http://dbis.rwth-aachen.de/~derntl/papers/preprints/dasp2013-dvita-preprint.pdf) for details). This program will rank the topics and store the result in the DB, so the app can display pre-sorted lists.

Command template:
`java TopicRanking TOPICMINING_ID`

* TOPICMINING_ID is the actual ID from CONFIG_TOPICMINING.ID

Example:
`java TopicRanking 40`

###5. Document Similarity Computation
DVITA allows users to browse documents by similarity over different time slices in the topic model. Details of how this is done are in [this paper](http://dbis.rwth-aachen.de/~derntl/papers/preprints/dasp2013-dvita-preprint.pdf).

Command template:
`java SimilarDocumentComputation TOPICMINING_ID [NUM_PERIODS]`

* TOPICMINING_ID is the actual ID from CONFIG_TOPICMINING.ID
* NUM_PERIODS is an optional paramter (default = 1), allowing to set the number of time periods (in either direction) to go when computing similar documents. Be very careful with larger datasets and numbers of periods because it will produce one record in the similarities table in the DB for each period and each document pair!

Example:
`java SimilarDocumentComputation 40`

###6. Add Topic Model to DVITA Web App
Now we can add the newly built dynamic topic model to the DVITA web app for users to explore.

In the CONFIG_GUI table, each row represents a group node in the DVITA Data Set selection dialog. The topic models to show under the group node are referenced in the field MININGIDS, which refer to space separated identifiers from CONFIG_TOPICMINING.ID

If you skip this last step, the topic model will still be available for exploration by supplying its ID as an URL parameter to the DVITA Web app, e.g. http://[YOUR SERVER]/DVita?id=40.

##Notes
###DVITA2
There is an experimental [DVITA2](https://github.com/rwth-acis/DVITA2) app, which encapsulates the OfflineComponents in a web based control panel GUI.

###License
Copyright 2012-2015 Michael Derntl, Nikou Günnemann

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
