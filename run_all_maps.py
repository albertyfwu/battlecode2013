import subprocess
import re

games = 0
wins = {}

p = subprocess.Popen(['ant', 'file'], shell=True, stdout=subprocess.PIPE)
for line in iter(p.stdout.readline, ''):
    print line
    # check if somebody won
    m = re.search("([A-Za-z0-9]+) \([A|B]\) wins", line)
    if m:
        # somebody won
        games += 1
        winner = m.groups()[0]
        if winner in wins:
            wins[winner] += 1
        else:
            wins[winner] = 1

print 'Wins (out of %s games):' % games
for winner in wins:
    print winner + ': ' + str(wins[winner])
    
p.stdout.close()
