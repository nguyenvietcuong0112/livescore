import urllib.request
import json
import urllib.parse
from Crypto.Cipher import AES
import base64

base_url = "https://api.1teps.com/livescore/"
api_key = "86cae86b105350834620f2888fe1445e"
package_name = "com.livescore.football.livescores.footballscores"
version_code = "3"

passphrase = f"{package_name}__{version_code}__Live_Lcore"

def pad(s):
    return s + b"\0" * (16 - len(s) % 16)

def decrypt_aes_256(enc_base64, key_str):
    # Java implementation of decryptAES256 in CryptoUtils:
    # Uses MD5 of passphrase to derive key and IV? Or uses PBKDF2?
    # Let's look at CryptoUtils.kt to understand the decryption logic!
    pass
