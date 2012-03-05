class APIClient:
    """Generic API client for Swagger client library builds"""

    def __init__(self, api_key=None, api_server=None):
        if api_key == None:
            raise Exception('You must pass an api_key when instantiating the '
                            'APIClient')
        self.api_key = api_key
        self.api_server = api_server