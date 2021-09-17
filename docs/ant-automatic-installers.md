# Ant Automatic Installers

Choose an automatic installer to let Jenkins install this tool for you on demand.
If you check this option, you'll then be asked to configure a series of "installer"s for this tool, where each installer defines how Jenkins will try to install this tool.

For a platform-independent tool (such as Ant), configuring multiple installers for a single tool does not make much sense, but for a platform dependent tool, multiple installer configurations allow you to run a different set up script depending on the agent environment.

## Install from Apache

This installs Ant from [apache.org](https://ant.apache.org/) using one of the publicly available versions.

### Configuration Options

* Version: the ant version to install from apache.org.

## Extract \*.zip/\*.tar.gz

Downloads a tool archive and installs it within Jenkins's working directory. Example: http://apache.promopeddler.com/ant/binaries/apache-ant-1.7.1-bin.zip (or whatever mirror is closest to your server) and specify a subdir of apache-ant-1.7.1.

### Configuration options

* Label - a label for this installer
* Download URL for binary archive - URL do download the binary archive from
* Subdirectory of extracted archive - directory in the extracted archive containing the tools to install

## Run Batch Command

Runs a shell command of your choice to install the tool. Ubuntu example, assuming the Jenkins user is in /etc/sudoers:

```sh
sudo apt-get --yes install openjdk-6-jdk
```
(In this case specify e.g. /usr/lib/jvm/java-6-openjdk-i386 as the home directory.)

As another example, to install an older version of Sun JDK 6 for (x86) Linux, you can use the obsolete DLJ:

```sh
bin=jdk-6u13-dlj-linux-i586.bin
if [ \! -f $bin ]
then
    wget --no-verbose http://download.java.net/dlj/binaries/$bin
    sh $bin --unpack --accept-license
fi
```
(In this case specify jdk1.6.0_13 as the home directory.)

### Configuration Options

* Label - a label for this installer
* Command - command(s) to execute in a shell script
* Tools home - home directory for the installed tool

## Run Shell Command

Runs a shell command of your choice to install the tool. Ubuntu example, assuming the Jenkins user is in /etc/sudoers:

```sh
sudo apt-get --yes install openjdk-6-jdk
```
(In this case specify e.g. /usr/lib/jvm/java-6-openjdk-i386 as the home directory.)

As another example, to install an older version of Sun JDK 6 for (x86) Linux, you can use the obsolete DLJ:

```sh
bin=jdk-6u13-dlj-linux-i586.bin
if [ \! -f $bin ]
then
    wget --no-verbose http://download.java.net/dlj/binaries/$bin
    sh $bin --unpack --accept-license
fi
```
(In this case specify jdk1.6.0_13 as the home directory.)

### Configuration Options

* Label - a label for this installer
* Command - command(s) to execute in a shell script
* Tools home - home directory for the installed tool
