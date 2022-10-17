#!/bin/sh
# Note that the necessary jars should already be in the plugins/ directory as JAR files

# create a runner directory inside TraVarT
rm -fr travart-run
mkdir -p travart-run/plugins

# create a JAR of the app
mvn clean package -DskipTests

# copy required files into travart-run
cp plugins/*.jar travart-run/plugins
cp target/travart-runtime-*-SNAPSHOT.zip travart-run

# change the current directory
cd travart-run

# delete exting file
rm -f plugins/enabled.txt

# create a new enabled.txt
touch plugins/enabled.txt

# generate enabled.txt
delimiter="-plugin"
for filename in plugins/*.jar; do
  basefilename=$(basename -- "$filename")
  s=$basefilename$delimiter
  array=();
  while [[ $s ]]; do
      array+=( "${s%%"$delimiter"*}" );
      s=${s#*"$delimiter"};
  done;
  echo ${array}"-plugin" >> plugins/enabled.txt
done

# unzip app
jar xf travart-runtime-*.zip
rm travart-runtime-*.zip

# run travart
mv travart-runtime-*-SNAPSHOT.jar travart-runtime.jar
java -jar travart-runtime.jar
