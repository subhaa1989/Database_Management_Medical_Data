## database-frontend-for-medical-gate-analysis-data

Steps to be followed to deploy the application in local:
1. Database configurations
	1.1. Create a data schema with name "medical_gate_analysis"
	1.2. Change the database username and password in /src/main/resources/hibernate.cfg.xml to deploy in local machines.
2. javax.servlet-api-3.0.1.jar file available in /src/main/resources should be placed in /WebContent/WEB-INF/lib in local.
3. Export the application as war file with name "medicalgateanalysis.war".
4. Place the libclib_jiio.so file available in /src/main/resources into the java.library.path, say for example "/usr/lib". (In Windows, skip this step)
5. Place the medicalgateanalysis.war file into the tomcat webapps folder. Or deploy the application through tomcat manager. 
6. Start/restart the tomcat server. (Optional)
7. Access the application home page at "http://<hostname>:<port>/medicalgateanalysis"

Steps to be followed to deploy the application in bay VM:
1. Database configurations
	1.1. Create a data schema with name "medical_gate_analysis"
	1.2. Comment the lines 12, 13 and 14 and uncomment the lines 15 to 20 in /src/main/resources/hibernate.cfg.xml to deploy in bay virtual 		 machine or in test server.
2. Export the application as war file with name "medicalgateanalysis.war".
3. Place the libclib_jiio.so file available in /src/main/resources into the java.library.path, say for example "/usr/lib".
4. Place the medicalgateanalysis.war file into the tomcat webapps folder. In bay VM, into /var/lib/tomcat/webapps.
5. Run the following commands on terminal to give proper permission to the war file
	chmod 755 medicalgateanalysis.war
	chown root medicalgateanalysis.war
	chgrp tomcat medicalgateanalysis.war 
6. Start/restart the tomcat server.
7. Run the following command on terminal to give proper permission to the deployed application
	chown root medicalgateanalysis
8. Restart the tomcat server.
9. Access the application home page at "https://<hostname>:<port>/medicalgateanalysis"