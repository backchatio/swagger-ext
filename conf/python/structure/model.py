class Model(dict):

	def __init__(self, data):
		dict.__init__(self, data)

	def __getitem__(self, key):
		if dict.has_key(self,key): 
			return dict.__getitem__(self, key)
		else:
			return None

	def __setitem__(self, key, val):
		pass

	def __getattr__(self, name):
		return self.__getitem__(name)

	def __setattr__(self, name, val):
		return self.__setitem__(name, val)