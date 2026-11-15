#!/usr/bin/env python3
"""Tiny App Store Connect API client: a signed request from the command line, no dependencies.

Uses the Developer-role key at ~/private_keys/AuthKey_P6GP679JH9.p8 (never in the repo) and
openssl for the ES256 signature. Examples:

    python3 ios/asc_api.py GET "/v1/apps/6834949463/reviewSubmissions"
    python3 ios/asc_api.py GET "/v1/apps/6834949463/appStoreVersions?fields[appStoreVersions]=versionString,appVersionState"
    python3 ios/asc_api.py PATCH "/v1/appStoreVersions/<id>" '{"data":{"type":"appStoreVersions","id":"<id>","attributes":{"releaseType":"MANUAL"}}}'

Prints the HTTP status, then the JSON. Reference: https://developer.apple.com/documentation/appstoreconnectapi
"""
import base64, json, os, subprocess, sys, time, urllib.request, urllib.error

KEY_ID = "P6GP679JH9"
ISSUER = "5c063e03-6ea7-439a-8d45-3b9e0cabb78a"
KEY_PATH = os.path.expanduser("~/private_keys/AuthKey_%s.p8" % KEY_ID)

def b64u(b): return base64.urlsafe_b64encode(b).rstrip(b"=").decode()

def der_to_raw(der):
    # SEQUENCE { INTEGER r, INTEGER s }
    i = 2 if der[1] < 0x80 else 3
    assert der[i] == 2; l = der[i+1]; r = der[i+2:i+2+l]; i += 2+l
    assert der[i] == 2; l = der[i+1]; s = der[i+2:i+2+l]
    r = r.lstrip(b"\x00").rjust(32, b"\x00"); s = s.lstrip(b"\x00").rjust(32, b"\x00")
    return r + s

def token():
    now = int(time.time())
    h = b64u(json.dumps({"alg":"ES256","kid":KEY_ID,"typ":"JWT"}).encode())
    p = b64u(json.dumps({"iss":ISSUER,"iat":now,"exp":now+900,"aud":"appstoreconnect-v1"}).encode())
    msg = (h + "." + p).encode()
    der = subprocess.run(["openssl","dgst","-sha256","-sign",KEY_PATH], input=msg, capture_output=True, check=True).stdout
    return h + "." + p + "." + b64u(der_to_raw(der))

def call(method, path, body=None):
    cmd = ["curl","-sg","-o","/dev/stdout","-w","\n%{http_code}","-X",method,
           "-H","Authorization: Bearer "+token(),"-H","Content-Type: application/json",
           "https://api.appstoreconnect.apple.com"+path]
    if body is not None: cmd += ["-d", json.dumps(body)]
    out = subprocess.run(cmd, capture_output=True, text=True).stdout
    data, _, code = out.rpartition("\n")
    return int(code), (json.loads(data) if data.strip() else {})

if __name__ == "__main__":
    method, path = sys.argv[1], sys.argv[2]
    body = json.loads(sys.argv[3]) if len(sys.argv) > 3 else None
    status, data = call(method, path, body)
    print(status); print(json.dumps(data, indent=1)[:4000])
