# How to run 
```bash
./gradlew jar
# MarathonCodeEvaluator template command (local)
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeEvaluator --source LOCAL --logInputDir path/to/dir 
# MarathonCodeEvaluator template command (s3)
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeEvaluator --source S3 --s3bucket S3 --contestName ahc25

# MarathonCodeSimulatro template command
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeSimulator --commandTemplate 'java Main $SEED' --minSeed 123 --maxSeed 222 --s3 --dynamoDbUpdateType PRODUCTION --contestName httf-2020-final
```
