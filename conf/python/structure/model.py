class Model(dict):

	def __init__(self, data):
		dict.__init__(self, data)

	def __getitem__(self, key):
		if dict.has_key(self,key): 
			val  = dict.__getitem__(self, key)
			return to_data(val)
		else:
			return None

	def __setitem__(self, key, val):
		pass

	def __getattr__(self, name):
		return self.__getitem__(name)

	def __setattr__(self, name, val):
		return self.__setitem__(name, val)

def to_data(data):
	typ = type(data)
	if typ in [unicode, str, int, bool, float]:
		return data
	elif typ == list:
		return map(lambda x: to_data(x), data)
	else:
		return Model(data)