@echo ff
pushd .
call mvn -DskipTests clean install
cd ..\jagent
call mvn clean install
popd

