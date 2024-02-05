# Define the path to your POM XML file
$xmlFilePath = "pom.xml"

# Load the XML file
[xml]$xml = Get-Content $xmlFilePath

# Extract the current version
$currentVersion = $xml.project.version

# Check if the version contains -SNAPSHOT
if ($currentVersion -match ".*-SNAPSHOT.*") {
    Write-Host "Version contains -SNAPSHOT. Skipping version increment."
} else {
    # Define a regex pattern to match the version format <version>...</version>
    $pattern = '(<version>.*?)(\d+)\.(\d+)\.(\d+)(.*?</version>)'

    # Find the version using regex
    if ($currentVersion -match $pattern) {
        $prefix = $matches[1]
        $major = $matches[2]
        $minor = $matches[3]
        $patch = [int]$matches[4] + 1
        $suffix = $matches[5]

        # Create the updated version
        $newVersion = "$prefix$major.$minor.$patch$suffix"

        # Replace the old version with the new version in the XML
        $xml.project.version = $newVersion

        # Save the updated XML back to the file
        $xml.Save($xmlFilePath)

        # Display the updated version
        Write-Host "Updated version: $($xml.project.version)"
    } else {
        Write-Host "Version format not found or does not match the expected pattern."
    }
}
