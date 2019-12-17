# HolySheet

HolySheet is a program that allows you to store arbitrary files onto Google Sheets, which does not lower storage quota on Google Drive. This is inspired by [uds](https://github.com/stewartmcgown/uds), however it can only store ~710KB of data per doc due to the use of Base64 and Docs limitations, and only has CLI usage.

HolySheet uses Google Sheets, which has an undocumented maximum 25.9MB* of data capacity in my less-than-professional testing. A modified Base91 algorithm is also used to efficiently convert arbitrary files into text to work with Sheets. Compression to Zip is also offered, with other compression methods planned.

\* This could be more, it arbitrarily throws 500 ISE's at upload requests with more (Sometimes the limit may need to be lowered temporarily as it doesn't like some sizes sometimes)

Currently the program is CLI-based, with GUI in development. 

Usage:

![Usage Screenshot](https://rubbaboy.me/images/e6exbja)

Remote file listing:

![File listing](https://rubbaboy.me/images/6ppwqgi)