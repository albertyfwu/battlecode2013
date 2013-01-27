import subprocess
import re

games = 0
wins = {}

p = subprocess.Popen(['ant', 'file'], shell=True, stdout=subprocess.PIPE)

current_map = 0

for line in iter(p.stdout.readline, ''):
    # check if somebody won
    m1 = re.search("[A-Za-z0-9]+ vs\. [A-Za-z0-9]+ on ([A-Za-z0-9]+)", line)
    if m1:
        # a game has started
        current_map = m1.groups()[0]
        
    m2 = re.search("([A-Za-z0-9]+) \([A|B]\) wins", line)
    if m2:
        # somebody won
        games += 1
        winner = m2.groups()[0]
        if winner in wins:
            wins[winner] += 1
        else:
            wins[winner] = 1
        print 'Games completed: %d, map: %s, winner: %s' % (games, current_map, winner)
        print wins
        print ''

print ''
print '----------'
print ''
print 'Wins (out of %s games):' % games
for winner in wins:
    print winner + ': ' + str(wins[winner])
    
p.stdout.close()
