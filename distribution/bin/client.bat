@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
for %%i in ("%~dp0..") do set "BASEDIR=%%~fi"

:repoSetup
set REPO=


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\lib

set CLASSPATH="%BASEDIR%"\conf;"%REPO%"\natproxy-server-1.0-SNAPSHOT.jar;"%REPO%"\natproxy-core-1.0-SNAPSHOT.jar;"%REPO%"\netty-all-4.1.90.Final.jar;"%REPO%"\netty-buffer-4.1.90.Final.jar;"%REPO%"\netty-codec-4.1.90.Final.jar;"%REPO%"\netty-codec-dns-4.1.90.Final.jar;"%REPO%"\netty-codec-haproxy-4.1.90.Final.jar;"%REPO%"\netty-codec-http-4.1.90.Final.jar;"%REPO%"\netty-codec-http2-4.1.90.Final.jar;"%REPO%"\netty-codec-memcache-4.1.90.Final.jar;"%REPO%"\netty-codec-mqtt-4.1.90.Final.jar;"%REPO%"\netty-codec-redis-4.1.90.Final.jar;"%REPO%"\netty-codec-smtp-4.1.90.Final.jar;"%REPO%"\netty-codec-socks-4.1.90.Final.jar;"%REPO%"\netty-codec-stomp-4.1.90.Final.jar;"%REPO%"\netty-codec-xml-4.1.90.Final.jar;"%REPO%"\netty-common-4.1.90.Final.jar;"%REPO%"\netty-handler-4.1.90.Final.jar;"%REPO%"\netty-transport-native-unix-common-4.1.90.Final.jar;"%REPO%"\netty-handler-proxy-4.1.90.Final.jar;"%REPO%"\netty-handler-ssl-ocsp-4.1.90.Final.jar;"%REPO%"\netty-resolver-4.1.90.Final.jar;"%REPO%"\netty-resolver-dns-4.1.90.Final.jar;"%REPO%"\netty-transport-4.1.90.Final.jar;"%REPO%"\netty-transport-rxtx-4.1.90.Final.jar;"%REPO%"\netty-transport-sctp-4.1.90.Final.jar;"%REPO%"\netty-transport-udt-4.1.90.Final.jar;"%REPO%"\netty-transport-classes-epoll-4.1.90.Final.jar;"%REPO%"\netty-transport-classes-kqueue-4.1.90.Final.jar;"%REPO%"\netty-resolver-dns-classes-macos-4.1.90.Final.jar;"%REPO%"\netty-transport-native-epoll-4.1.90.Final-linux-x86_64.jar;"%REPO%"\netty-transport-native-epoll-4.1.90.Final-linux-aarch_64.jar;"%REPO%"\netty-transport-native-kqueue-4.1.90.Final-osx-x86_64.jar;"%REPO%"\netty-transport-native-kqueue-4.1.90.Final-osx-aarch_64.jar;"%REPO%"\netty-resolver-dns-native-macos-4.1.90.Final-osx-x86_64.jar;"%REPO%"\netty-resolver-dns-native-macos-4.1.90.Final-osx-aarch_64.jar;"%REPO%"\jboss-marshalling-2.1.1.Final.jar;"%REPO%"\jboss-marshalling-river-2.1.1.Final.jar;"%REPO%"\slf4j-api-2.0.7.jar;"%REPO%"\slf4j-reload4j-2.0.7.jar;"%REPO%"\reload4j-1.2.22.jar;"%REPO%"\log4j-core-2.20.0.jar;"%REPO%"\log4j-api-2.20.0.jar;"%REPO%"\lombok-1.18.26.jar;"%REPO%"\snakeyaml-1.21.jar;"%REPO%"\natproxy-client-1.0-SNAPSHOT.jar;"%REPO%"\distribution-1.0-SNAPSHOT.pom

set ENDORSED_DIR=
if NOT "%ENDORSED_DIR%" == "" set CLASSPATH="%BASEDIR%"\%ENDORSED_DIR%\*;%CLASSPATH%

if NOT "%CLASSPATH_PREFIX%" == "" set CLASSPATH=%CLASSPATH_PREFIX%;%CLASSPATH%

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% -Xms128M -Xmx128M -Xss256k -classpath %CLASSPATH% -Dapp.name="client" -Dapp.repo="%REPO%" -Dapp.home="%BASEDIR%" -Dbasedir="%BASEDIR%" org.github.ponking66.ClientApplication %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
