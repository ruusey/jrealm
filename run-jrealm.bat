start java -jar ../jrealm-data/target/jrealm-data.jar
timeout 8 > NUL
start java -jar ./target/jrealm.jar -embedded 127.0.0.1 ru@jrealm.com password 146cdcbd-4266-4148-baef-4381eb22f4ad
pause
