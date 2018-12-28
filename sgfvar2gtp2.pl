#!/usr/bin/perl

use warnings;
use strict;

my @ranks;

sub printgame
{
	my ($sgf, $result) = @_;
	my $pt = $sgf->getAddress();

	my @moves;
	do {
		my ($b, $w) = ($sgf->property('B'), $sgf->property('W'));
		if ($b) { push @moves, ['b', $_] foreach @$b; }
		if ($w) { push @moves, ['w', $_] foreach @$w; }
	} while ($sgf->prev());

	print "boardsize 19\nclear_board\n";
	for my $move (reverse @moves) {
		my ($sx, $sy) = @{$move->[1]};
		my @abcd = split(//, "abcdefghjklmnopqrstuvwxyz");
		my $x; my $y;
		if ($sx eq "" || $sy eq "") {
			$x = 20; $y = "z";
		} else {
			$x = 19 - $sy; $y = $abcd[$sx];
		}
		if ("$y$x" eq "z20") {
			$y = "pass"; $x = "";
		}
		print "play ".$move->[0]." $y$x\n";

		# my $rank;
		# if ($move->[0] eq "b") {
		# 	$rank = $ranks[0];
		# } else {
		# 	$rank = $ranks[1];
		# }
		print "_dump $result\n";
	}

	$sgf->goto($pt);
}

sub replaygame
{
	my ($sgf, $result) = @_;
	my $pt = $sgf->getAddress();

	my @moves;
	do {
		my ($b, $w) = ($sgf->property('B'), $sgf->property('W'));
		if ($b) { push @moves, ['b', $_] foreach @$b; }
		if ($w) { push @moves, ['w', $_] foreach @$w; }
	} while ($sgf->prev());

	print "boardsize 19\nclear_board\n";
	for my $move (reverse @moves) {
		my ($sx, $sy) = @{$move->[1]};
		my @abcd = split(//, "abcdefghjklmnopqrstuvwxyz");
		my $x; my $y;
		if ($sx eq "" || $sy eq "") {
			$x = 20; $y = "z";
		} else {
			$x = 19 - $sy; $y = $abcd[$sx];
		}
		if ("$y$x" eq "z20") {
			$y = "pass"; $x = "";
		}
		print "play ".$move->[0]." $y$x\n";

		# my $rank;
		# if ($move->[0] eq "b") {
		# 	$rank = $ranks[0];
		# } else {
		# 	$rank = $ranks[1];
		# }
		# print "features_planes_file $result\n";
	}

	$sgf->goto($pt);
}

sub recurse
{
	my ($sgf, $result, $type) = @_;
	# my $c = $sgf->property('C');
	# #	if ($c and $c->[0] =~ /GOOD/) {
	# if ($c) {
	# 	#printgame($sgf);
	# }
	while ($sgf->branches()>0) {
		# print "barnch ".$sgf->branches();
		$sgf->gotoBranch(0);
	}
	if ($type eq 0) {
		replaygame($sgf, $result);
	} elsif ($type eq 1) {
		printgame($sgf, $result);
	} else {
		print STDERR "bad type\n";
		abort();
	}
}

use Games::SGF::Go;
#my @files = glob "*.sgf";
use IO::Handle;
STDOUT->autoflush(1);

#foreach my $file (@files)
my $file = $ARGV[0];
{
	my $sgf = new Games::SGF::Go;

	print STDERR $file."\n";
	$sgf->readFile($file);

	my $br__ = $sgf->property('BR');
	my $wr__ = $sgf->property('WR');
	my $result__ = $sgf->property('RE');
	my $komi__ = $sgf->property('KM');
	my $handicap__ = $sgf->property('HA');
	#print STDERR $br__ . "\n";
	#print STDERR $wr__ . "\n";

	if ($result__ eq 0) {
		print STDERR "no result\n";
		next;
	}
	if ($handicap__ ne 0) {
		print STDERR "handicap game" . $handicap__->[0] . "\n";
		next;
	}
	if ($komi__ eq 0) {
		print STDERR "no komi\n";
		next;
	}
	my $komi = $komi__->[0];
	if (substr($komi, 0, 3) eq "6.5"
		or substr($komi, 0, 3) eq "7.5"
		or substr($komi, 0, 3) eq "5.5"
		or substr($komi, 0, 3) eq "2.75"
		or substr($komi, 0, 3) eq "3.75") {
		print STDERR $komi . " komi\n";
	} else {
		print STDERR $komi . " is not target komi\n";
		next;
	}

	# my @br_ = $br__[0];
	# my @wr_ = $wr__[0];
	# print STDERR $#br_ . "\n";
	# print STDERR $#wr_ . "\n";
	# if ($#br_ < 1 or $#wr_ < 1) {
	# 	next;
	# }
	# @ranks = ($br_[0], $wr_[0]);

	if ($br__ eq 0 or $wr__ eq 0) {
		print STDERR "unknown rank\n";
		# next;
	}
	# @ranks = ($br__->[0], $wr__->[0]);
	# print STDERR $ranks[0] . " vs " . $ranks[1] . "\n";
	# if (($ranks[0] =~ /([1234]d|.*k)/)
	# 	or ($ranks[1] =~ /([1234]d|.*k)/)) {
	# 	# print STDERR "skip\n";
	# 	# next;
	# }
	my $result = $result__->[0];
	if ($result =~ /^[^BW].*/) {
		print STDERR $result . " no result skip\n";
		next;
	}
	$result = substr($result, 0, 1);

	# . $sgf->property('WR')[0] . "\n";
	$sgf->gotoRoot();
	print "_clear\n";
	recurse($sgf, $result, 0);
	print "_store\n";
	recurse($sgf, $result, 1);
}
