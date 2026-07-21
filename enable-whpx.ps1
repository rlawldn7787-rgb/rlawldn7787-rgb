dism /online /enable-feature /featurename:HypervisorPlatform /all /norestart > "C:\Users\??\woohaeng-board\whpx-enable.log" 2>&1
dism /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart >> "C:\Users\??\woohaeng-board\whpx-enable.log" 2>&1
echo DONE>> "C:\Users\??\woohaeng-board\whpx-enable.log"
