import urllib
import urllib2
import httplib
import json
from .model import Model, to_data

class APIClient:

    def __init__(self, api_auth=None, api_server=None):
        if api_auth == None:
            raise Exception('You must pass an api_key when instantiating the APIClient')
        self.api_auth = api_auth
        self.api_server = api_server

    def submit(self, method, path, query_params = [], header_params = {}, path_params = {}):
        def to_list(key, value):
            params = []
            if value != None:
                typ = type(value)
                if typ in [str, int, float, bool]:
                    params.append((key, str(value)))
                elif typ == list:
                    for v in value:
                        params.extend(to_list(key, v))
                elif typ == tuple:
                    params.extend(to_list(key + '.'+ v[0], v[1]))
                elif typ == dict: # TODO json
                    for (k, v) in value:
                        params.extend(to_list(k, v))
            return params

        def encode_params(params):
            filtered = []
            for (param, value) in params:
                filtered.extend(to_list(param, value))
            return urllib.urlencode(filtered)

        for k, v in path_params.iteritems():
            path = path.replace('{%s}'%k, v)

        url = self.api_server + path

        headers = {}

        if self.api_auth:
            for param, value in self.api_auth.iteritems():
                headers[param] = value

        if method in ['GET', 'DELETE']:
            if query_params:
                url = url + '?' + encode_params(query_params)
            request = urllib2.Request(url=url, headers=headers)
            if method == 'DELETE':
                request.get_method = lambda: method
        elif method in ['POST', 'PUT']:
            data = None
            if query_params:
                data = encode_params(query_params)
            request = urllib2.Request(url=url, headers=headers, data=data)
            if method == 'PUT':
                request.get_method = lambda: method

        response = None
        try:
            response = urllib2.urlopen(request)
        except urllib2.HTTPError, e:
            response = e

        return Response(response)

class Response:

    def __init__(self, response):
        self.code = response.code
        try:
            self.content = json.loads(response.read())
        except ValueError:
            self.content = None

    def has_errors(self):
        return len(get_errors()) > 0

    def get_errors(self):
        if self.content.has_key('errors'):
            return self.content['errors']
        else:
            return []

    def get_data(self):
        d = None
        if self.content.has_key('data'):
            d = self.content['data']
        if d != None:
            return to_data(d)
        return d

    data = property(get_data)
    errors = property(get_errors)