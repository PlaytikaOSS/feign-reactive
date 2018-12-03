To run benchmarks on Windows increase the number of ephemeral ports 

https://support.microsoft.com/en-ca/help/196271/when-you-try-to-connect-from-tcp-ports-greater-than-5000-you-receive-t

and to take effect also adjust the following parameters:

To tweak TCP timeouts we adjusted the following parameters:

[HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters]
"TcpTimedWaitDelay"=dword:00000028

[HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\Tcpip\Parameters]
"StrictTimeWaitSeqCheck"=dword:00000001

While changing this parameter the following important points needs to be considered:

Changing these values requires a reboot. Plan to do that out of your production hours.
TcpTimedWaitDelay is 2 minutes by default, even if the value is not present in the registry.
You must set the StrictTimeWaitSeqCheck to 0x1 or the TcpTimedWaitDelay value will have no effect.
