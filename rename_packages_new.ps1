# PowerShell script to update package names from io.mindsync.app to com.mindsynclabs.focusapp

$oldPackage = "io.mindsync.app"
$newPackage = "com.mindsynclabs.focusapp"

# Find all Kotlin and Java files in the project
$files = Get-ChildItem -Path "app/src" -Recurse -Include "*.kt", "*.java", "*.xml"

foreach ($file in $files) {
    Write-Host "Processing $($file.FullName)"
    
    # Read the file content
    $content = Get-Content -Path $file.FullName -Raw
    
    # Replace package declarations
    $newContent = $content -replace "package\s+$oldPackage", "package $newPackage"
    
    # Replace import statements
    $newContent = $newContent -replace "import\s+$oldPackage", "import $newPackage"
    
    # Check if content was modified
    if ($content -ne $newContent) {
        Write-Host "  Modified $($file.FullName)"
        # Write the updated content back to the file
        Set-Content -Path $file.FullName -Value $newContent -NoNewline
    }
}

Write-Host "Package renaming completed!" 