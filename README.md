# rf2-diff-generator
A process to generate differences reports between two SNOMED CT releases.

This project is an extraction of utilities previously available in the IHTSDO Workbench codebase.

It is mainly used for the daily build browser. Run the following command to generate the diff report json

java -Xmx3g -jar rf2-diff-generator-1.0-jar-with-dependencies.jar config.xml

The config.xml looks like this:

<?xml version="1.0" encoding="UTF-8"?>
<config>
    <inputFullFileDirectory>/opt/dailybuild/SnomedCT_RF2Release_INT_20160731/Full</inputFullFileDirectory>
    <diffReportOutputDirectory>/opt/dailybuild/diff-reports</diffReportOutputDirectory>
    # One date after the previous release
    <startDate>20160201</startDate>
    <currentReleaseDate>20160731</currentReleaseDate>
    <editionName>International</editionName>
</config>

