all: run

INPUT_FILE ?= input.txt
WORKERS    ?= 1

clean:
	rm -f out/WordCountPARCS.jar

out/WordCountPARCS.jar: out/parcs.jar src/WordCountPARCS.java
	@mkdir -p temp
	@javac -cp out/parcs.jar -d temp src/WordCountPARCS.java
	@jar cf out/WordCountPARCS.jar -C temp .
	@rm -rf temp/

build: out/WordCountPARCS.jar

run: out/WordCountPARCS.jar
	@echo ">> Launching WordCountPARCS with INPUT_FILE=$(INPUT_FILE), WORKERS=$(WORKERS)"
	@cd out && java -cp 'parcs.jar:WordCountPARCS.jar' src.WordCountPARCS $(INPUT_FILE) $(WORKERS) > ../run.txt 2>&1 || true
	@echo ">> Logs (full content of run.txt):"
	@cat run.txt