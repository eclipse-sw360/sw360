REM
REM Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
REM
REM All rights reserved. This configuration file is provided to you under the
REM terms and conditions of the Eclipse Distribution License v1.0 which
REM accompanies this distribution, and is available at
REM http://www.eclipse.org/org/documents/edl-v10.php
REM

keytool -list -rfc --keystore jwt.jks | openssl x509 -inform pem -pubkey
