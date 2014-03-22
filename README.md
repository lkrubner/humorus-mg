
*Assumptions*

This app assumes that you keep all of your data in MongoDB, stored in a single collection inside a single database. The schema.edn file specifies a "schema" in the sense that it specifies HTML forms, and those forms will, almost accidentally, determine which keys will exist in your database. The system is extemely flexible: you can create any schema by changing the schema.edn file. As such, I believe this app could be used in a great variety of situations, for a great variety of companies.

*Theory*

This project started life as the admin app for the website TailorMadeAnswers.com. I am building TailorMadeAnswers.com as a series of apps that work together. You can get a very rough idea of the architecture if you wade through the verbiage here: 

http://www.smashcompany.com/technology/an-architecture-of-small-apps

This app is by far the most monstrous that I will allow. This app connects to the database, and it also listens on port 80 for TCP/IP connections. Most of the other apps I write do one or the other, not both. But an admin dashboard CMS needs to be a little bit monstrous. 

This app is enabled by other apps. For instance, this app will upload images but it will not generate thumbnails of those images. It assumes some other app is making thumbnails. And here is the app I use to make thumbnails:

https://github.com/lkrubner/tma_make_thumbnails


*Things that will never change about this app*

1.) This app will never have more than 2,000 lines of Clojure code. None of my apps will ever have more than 2,000 lines of code. If you find yourself needing more than 2,000 lines of code, then you should create a new app. Keep your apps small. Compile a file and include it as a library if you must (and then, put it in a separate Git repo). 

2.) This app connects to MongoDB. It will never connect to some other type of database. We do not need an ORM. We can hardcode everything to MongoDB.

3.) This app supports one level of user, a root level "admin" that has the power to do anything. This app will never support multiple levels of users permissions. I feel strongly that if you want to support another level of user permissions, then you should create a new app. If, for instance, you wanted to create an "editor" who could edit pages but not delete anything, then you should create a new app where all the queries are adjusted to limit the powers of the "editor". Different apps for different levels. Don't try to stuff all that complexity into 1 app. 

4.) This app does not support all CRUD operations. It supports C and R and D but not U. Partly inspired by Clojure itself, we will not ever support any form of "update in place". Most of the apps assoicated with TailorMadeAnswers.com can only do reads and inserts. Certainly, any normal user of the public site can only do reads and inserts. This app allows deletes, since it is the root level user, and sometimes (as when a password is posted to a public page) a document needs to be deleted. But there are never any updates, and there never will be any updates. 


*There are several mistakes in this app*

1.) I hard-coded the use of "tma" as the name of my collection in MongoDB. I will change this soon. (All hard-coded references to TMA will eventually be changed.)

2.) I hard-coded exactly 1 user account in server.clj. Obviously, I hope that soon we can populate the var "users" with users drawn from the database.

3.) tma-admin.server/change-current-logging-level  is no longer needed and will be removed. 

4.) tma-admin.server calls tma-admin.supervisor. I will reverse this soon. To a limited extent, the name "supervisor" was used in deference to Joe Armstrong's ideas about organizing code. http://www.erlang.org/download/armstrong_thesis_2003.pdf

5.) tma-admin.controller/get-map-of-properties-for-this-one-field uses (reduce) but I could have simply used (map) in tma-admin.controller/is-this-field-important. I'll try to fix this soon. 

6.) I hard-coded directory paths that I use on my MacBook Pro. Eventually this will get moved to a config file. 

7.) Using Lamina, I hard-coded the use of 6 workers because I knew my server had 4 CPUs. I will fix this soon. 

8.) I will eventually clean up this README. For now I am writing down ideas as fast as they occur to me.

I'm sure there are many others. I am still somewhat new to Clojure, so this app reveals some ideas that I have only half grasped or half implemented (for instance, I've started using Lamina for async, but not well). 


*Why I am excited about this app*

I posted a partial defense of MongoDB here:

http://www.smashcompany.com/technology/why-i-use-mongodb

What I suggested was that most early stage startups don't know what their schema will be, and they should not try to force the issue. I work with a lot of early stage companies, and the entrepreneurs I work have no idea what sets of data they will want to capture, or what the relationships will be among those sets of data. And I feel this app does a good job of allowing taking advantage of the flexibility that MongoDB affords. Simply change schema.edn and you've changed what sets of data you are capturing. To whatever extent I end up consulting for other startups, I hope to be able to give them the gift of a flexible initial system that does not lock them to a strict model that can not possibly match whatever relationship-constraints they will eventually discover.


*Setup*

You will need to apply some indexes to MongoDB. For me, these were crucial:

db.tma.ensureIndex( { "created-at":1, "user-item-name": 1 } );

db.tma.ensureIndex( { "item-name":1, "updated-at": 1 } );

I will create more in the future, but without these the software did not run. Your needs may vary based on what your schema is, but these are fields that are assumed (and mostly created) by default. 

Assuming you have Leiningen installed, you can build this app with: 

lein uberjar

You can start the app with something like:

java -jar target/admin-1-standalone.jar  30001 

or:

java -jar target/admin-1-standalone.jar  80 production

The number is the TCP/IP port that you want to listen on, obviously in production this will usually be 80, but you can set it to anything that your server will recognize. 

Of course, in production, you will want to daemonize the process, via whatever method you like best. I linked to some articles in the article above "An architecture of small apps", but if you need some ideas, here are some useful tidbits of information: 

http://www.smashcompany.com/technology/run-java-as-a-daemon

http://www.smashcompany.com/technology/how-to-run-shell-commands-in-the-background

http://www.smashcompany.com/technology/deploying-clojure-to-production

http://www.smashcompany.com/technology/how-to-restart-your-server-processes-when-they-die

http://www.smashcompany.com/technology/running-java-as-a-service-daemon-on-a-linux-machine



*The name*

MongoDB is a play on the word "humongous" so I wanted a name that started off sounding similar. However, this app is simple and lightweight, and delibrately limited, so I thought "humorous" would be a good name. However, I also wanted something that would be unique enough that people could find it via Goodle, so I settled on "humorus-mg". 





