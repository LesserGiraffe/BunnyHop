
(function() {
	return ['plink', '-ssh', ipAddr, '-l', uname, '-pw', password, 
			'pkill', '-f', execEnvironment];
})();