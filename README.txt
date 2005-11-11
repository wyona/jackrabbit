
  About
  =====

    - An ANT based environment to build JCR/Jackrabbit based applications
    - A utility class to import files and directories from the filesystem


  Requirements
  ============

    JDK-1.4.2 (e.g. export PATH=/usr/local/j2sdk1.4.2_10/bin:$PATH)
    Apache-Ant-1.6.5 (e.g. export PATH=/usr/local/apache-ant-1.6.5/bin:$PATH)
    Apache-Maven-1.0.2 (e.g. export PATH=/usr/local/maven-1.0.2/bin:$PATH)

    NOTE: Only tested on Linux yet


  Getting Started
  ===============

    Download required libraries: maven (or deprecated: ant download-libraries)
    Build Application:           ant build-app
    Show repository:             ant run-view
    Import file/directory:       ant run-import -Dfile=/foo/bar
    Show repository:             ant run-view
    Clean build:                 ant clean-build
