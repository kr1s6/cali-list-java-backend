# Documentation

## How to set up MongoDB
Insert into Environment Variables appropriate variables and values in Run Configurations:
````
MONGO_CLUSTER=*******;
MONGO_DATABASE_NAME=*******;
MONGO_PASSWORD=*******;
MONGO_USERNAME=*******;
If the username or password contains a colon (:) or an at-sign (@) then it must be urlencoded.
   @ → %40   : → %3A
````
