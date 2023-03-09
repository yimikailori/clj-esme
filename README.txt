
This Vendor specific ESME is written to convert vendor SMPP protocol to USSD
ESME is External Short Message Entity and used predominatly in telecommunication services.



To run
`lein uberjar`

You can use the command below

$JAVA_HOME/java -Dclojure.core.async.pool-size=200 -Dlogback.configuration=File:/opt/equinox/apps/esme/conf/logback.xml \
                      -cp '/opt/equinox/apps/esme/bin/clj_esmesmpp-1.1.0-SNAPSHOT-standalone.jar:/opt/equinox/apps/esme/libs/*' \
                      esme.core --config=/opt/equinox/apps/esme/conf/esmeprop.conf &


-Dclojure.core.async.pool-size=24 
set number of threads to 24 from default
