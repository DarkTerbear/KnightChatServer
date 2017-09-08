$original_dir = (Get-Item -Path ".\" -Verbose).FullName
cd C:\ProgramData\App-V\C87BE6C9-9379-431A-AE70-FD15E8F1AACA\BBA5E63B-FA99-4CF9-9FA8-1991602E58BC\root\VFS\ProgramFilesX64\eclipse\jre\bin
.\java -jar $original_dir\KnightChatServer.jar
cd $original_dir