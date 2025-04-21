all: run

INPUT_FILE ?= ex1.txt
WORKERS    ?= 4

clean:
	rm -f out/WordCount.jar

out/WordCount.jar: out/parcs.jar src/WordCountPARCS.java
	@mkdir -p temp
	@javac -cp out/parcs.jar -d temp src/WordCountPARCS.java
	@jar cf out/WordCount.jar -C temp .
	@rm -rf temp/

build: out/WordCount.jar

run: build
    @cd out && java -cp 'parcs.jar:WordCount.jar' src.WordCountPARCS $(INPUT_FILE) $(WORKERS)