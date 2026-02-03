@rem Gradle startup script for Windows.
@rem Uses Gradle wrapper; requires gradle/wrapper/gradle-wrapper.jar and gradle-wrapper.properties.

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_HOME=%DIRNAME%

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
echo ERROR: JAVA_HOME is not set and no 'java' command found in PATH.
exit /b 1

:findJavaFromJavaHome
set JAVA_EXE=%JAVA_HOME%\bin\java.exe
if exist "%JAVA_EXE%" goto execute
echo ERROR: JAVA_HOME is set but %JAVA_EXE% not found.
exit /b 1

:execute
if not exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" (
  echo ERROR: gradle\wrapper\gradle-wrapper.jar not found. Run from Android Studio once to generate wrapper, or add the jar manually.
  exit /b 1
)
"%JAVA_EXE%" -Dorg.gradle.appname=gradlew -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
exit /b %ERRORLEVEL%
