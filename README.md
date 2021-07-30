```bash
# MarathonCodeEvaluator template command
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeEvaluator --s3 --s3prefix httf-2020-final
# MarathonCodeSimulatro template command
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeSimulator --commandTemplate 'java Main $SEED' --minSeed 123 --maxSeed 222 --s3 --dynamoDbUpdateType PRODUCTION --contestName httf-2020-final
```