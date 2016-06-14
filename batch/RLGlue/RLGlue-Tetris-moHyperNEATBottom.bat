cd ..
cd ..
java -jar dist/MM-NEATv2.jar runNumber:%1 randomSeed:%1 base:tetris trials:3 maxGens:300 mu:50 io:true netio:true mating:true task:edu.utexas.cs.nn.tasks.rlglue.tetris.HyperNEATTetrisTask cleanOldNetworks:true fs:false noisyTaskStat:edu.utexas.cs.nn.util.stats.Average log:RL-moHyperNEATBottom saveTo:moHyperNEATBottom rlGlueEnvironment:org.rlcommunity.environments.tetris.Tetris rlGlueExtractor:edu.utexas.cs.nn.tasks.rlglue.featureextractors.tetris.RawTetrisStateExtractor tetrisTimeSteps:true tetrisBlocksOnScreen:true rlGlueAgent:edu.utexas.cs.nn.tasks.rlglue.tetris.TetrisAfterStateAgent hyperNEAT:true genotype:edu.utexas.cs.nn.evolution.genotypes.HyperNEATCPPNGenotype allowMultipleFunctions:true ftype:0 netChangeActivationRate:0.3 absenceNegative:true substrateMapping:edu.utexas.cs.nn.networks.hyperneat.BottomSubstrateMapping rlGluePort:4097