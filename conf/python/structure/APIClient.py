import urllib
import urllib2
import httplib
import json

class APIClient:

    def __init__(self, api_auth=None, api_server=None):
        if api_auth == None:
            raise Exception('You must pass an api_key when instantiating the APIClient')
        self.api_auth = api_auth
        self.api_server = api_server

    def submit(self, method, path, query_params = [], header_params = {}, path_params = {}):
        def encode_params(params):
            filtered = []
            for (param, value) in params:
                if value != None:
                    typ = type(value)
                    if typ in [str, int, float, bool]:
                        filtered.append((param, str(value)))
                    elif typ == list:
                        for e in value:
                            filtered.append((param, str(e))) # TODO
                    # TODO dict
            return urllib.urlencode(filtered)

        url = self.api_server + path

        headers = {}
        headers['Content-type'] = 'application/json'

        if self.api_auth:
            for param, value in self.api_auth.iteritems():
                headers[param] = value

        if method == 'GET':
            if query_params:
                url = url + '?' + encode_params(qp)
            request = urllib2.Request(url=url, headers=headers)
        elif method in ['POST', 'PUT', 'DELETE']:
            data = None
            if query_params:
                data = encode_params(query_params)
            request = urllib2.Request(url=url, headers=headers, data=data)
            if method in ['PUT', 'DELETE']:
                request.get_method = lambda: method

        code = None
        try:
            response = urllib2.urlopen(request)
        except urllib2.HTTPError, e:
            code = e.code
            response = e
        try:
            data = json.loads(response.read())
        except ValueError:
            data = None

        return (code, data)                
