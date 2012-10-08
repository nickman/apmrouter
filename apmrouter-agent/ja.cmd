@echo ff
pushd .
call mvn -DskipTests compile install
cd ..\jagent
call mvn clean install
popd

