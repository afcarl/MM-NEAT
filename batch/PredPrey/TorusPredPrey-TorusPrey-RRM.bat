cd ..
cd ..
java -jar dist/MM-NEATv2.jar runNumber:%1 randomSeed:%1 base:torusprey trials:10 maxGens:500 mu:100 io:true netio:true mating:false fs:false task:edu.utexas.cs.nn.tasks.gridTorus.TorusEvolvedPreyVsStaticPredatorsTask log:TorusPrey-RRM saveTo:RRM allowDoNothingActionForPredators:true torusPreys:2 torusPredators:3 staticPredatorController:edu.utexas.cs.nn.gridTorus.controllers.AggressivePredatorController preyRRM:true torusSenseByProximity:false torusSenseTeammates:true