# fyp-mcsts-ghosts
Final Year Project that uses Monte Carlo Tree Search to control Ghosts in Ms Pac-Man  

Files found in ms-pacman-engine/src/main/java/pacman/entries  

Credits:  
Ms Pacman Engine: https://github.com/solar-1992/PacManEngine  
Pacman MCTS: https://github.com/PanMig/MCTS-Pacman

To choose what is controlling the agents, choose the appropriate controller, i.e., AiGhosts, MctsPacman.    
Make sure to run only one game at a time.  
Also when running "Executor.runGameTimed()", make sure to change the 2nd paramter to the type of controller you're plugging in, i.e.,   
runRandomGameTimed(Controller<MOVE> pacManController, Legacy ghostController) for running original Ghosts in Ms Pac-Man  
runRandomGameTimed(Controller<MOVE> pacManController, AiGhosts ghostController) for running MCTS Ghosts  
runRandomGameTimed(Controller<MOVE> pacManController, RandomGhosts ghostController) for running Ghosts that move randomly  
