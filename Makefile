classpath = lib/commons-crypto-1.0.0.jar:lib/commons-lang3-3.7.jar:lib/jce1_2_2.jar:lib/sunjce_provider.jar
all:
	mkdir -p out
	javac -classpath ${classpath} src/main/java/*.java -d out/
	cd out/ && jar cfe inv-manager.jar main.java.InventoryManager main/java/*.class && echo '#!/usr/bin/env bash' > run.sh && echo 'java -jar inv-manager.jar' >> run.sh && chmod a+x run.sh
