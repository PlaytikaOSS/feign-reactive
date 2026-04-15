from flask import Flask, request, jsonify
import requests

app = Flask(__name__)

@app.route("/fetch")
def fetch():
    url = request.args.get("url")

    try:
        r = requests.get(url, timeout=5)

        return jsonify({
            "server_side_fetch": True,
            "requested_url": url,
            "status_code": r.status_code,
            "response_snippet": r.text[:200]
        })

    except Exception as e:
        return jsonify({
            "server_side_fetch": True,
            "requested_url": url,
            "error": str(e)
        })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
