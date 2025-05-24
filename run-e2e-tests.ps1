# Ejecutar tests E2E

& [mvnw.cmd](http://_vscodecontentref_/2) test -pl . "-Dspring.profiles.active=e2e" "-De2e.api-gateway.base-url=http://localhost:8080" "-Dtest=com.selimhorri.app.e2e.flows.*" "-DfailIfNoTests=false"
