import sys
import os

sys.path.append(os.path.dirname(os.path.abspath(__file__)) + '/../')
import model

class $resource$(object):

    def __init__(self, api_client):
        self.api_client = api_client